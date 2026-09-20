package info.isaksson.erland.repofleet.repository.inventory;

import info.isaksson.erland.repofleet.github.api.GitHubApiCallExecutor;
import info.isaksson.erland.repofleet.github.api.GitHubApiException;
import info.isaksson.erland.repofleet.github.api.GitHubApiFailureKind;
import info.isaksson.erland.repofleet.github.auth.GitHubInstallationTokenService;
import info.isaksson.erland.repofleet.github.client.GitHubContentItemResponse;
import info.isaksson.erland.repofleet.github.client.GitHubLicenseResponse;
import info.isaksson.erland.repofleet.github.client.GitHubRepositoryMetadataClient;
import info.isaksson.erland.repofleet.github.conditional.GitHubConditionalRequestExecutor;
import info.isaksson.erland.repofleet.repository.api.AnalysisState;
import info.isaksson.erland.repofleet.repository.api.LicensePresence;
import info.isaksson.erland.repofleet.repository.api.LicenseStatus;
import info.isaksson.erland.repofleet.repository.api.RepositorySummary;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.GenericType;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
class GitHubLicenseEnrichmentComponent {

    private final GitHubRepositoryMetadataClient client;
    private final GitHubApiCallExecutor apiCalls;
    private final GitHubConditionalRequestExecutor conditionalRequests;

    @Inject
    GitHubLicenseEnrichmentComponent(
            @RestClient GitHubRepositoryMetadataClient client,
            GitHubApiCallExecutor apiCalls,
            GitHubConditionalRequestExecutor conditionalRequests) {
        this.client = client;
        this.apiCalls = apiCalls;
        this.conditionalRequests = conditionalRequests;
    }

    GitHubLicenseEnrichmentComponent(
            GitHubRepositoryMetadataClient client,
            GitHubApiCallExecutor apiCalls) {
        this(client, apiCalls, null);
    }

    RepositoryMetadataResult<LicenseStatus> enrich(
            RepositorySummary repository,
            boolean cachedComplete) {
        LicenseStatus cachedLicense = repository.license();

        try {
            LicenseStatus license = conditionalRequests == null
                    ? refreshDirect(repository)
                    : refreshConditional(repository, cachedLicense, cachedComplete);
            return RepositoryMetadataResult.success(license);
        } catch (GitHubApiException exception) {
            if (exception.kind() == GitHubApiFailureKind.NOT_FOUND
                    && cachedLicense != null
                    && cachedLicense.presence() == LicensePresence.PRESENT) {
                return RepositoryMetadataResult.success(customLicense());
            }
            return fallback(cachedLicense, cachedComplete, exception);
        } catch (RuntimeException exception) {
            return fallback(cachedLicense, cachedComplete, exception);
        }
    }

    private LicenseStatus refreshConditional(
            RepositorySummary repository,
            LicenseStatus cachedLicense,
            boolean cachedComplete) {
        var contentsResult = conditionalRequests.execute(
                repository.id(),
                "root-contents",
                "root contents for " + repository.fullName(),
                Instant.now(),
                (authorization, etag) -> client.getRootContentsConditional(
                        repository.owner(),
                        repository.name(),
                        authorization,
                        GitHubInstallationTokenService.ACCEPT,
                        GitHubInstallationTokenService.API_VERSION,
                        etag),
                response -> response.readEntity(new GenericType<List<GitHubContentItemResponse>>() {}),
                () -> null);

        if (contentsResult.reusedCached()) {
            return cachedComplete ? cachedLicense : refreshDirect(repository);
        }

        List<GitHubContentItemResponse> contents =
                contentsResult.value() == null ? List.of() : contentsResult.value();
        if (!containsLicenseFile(contents)) {
            return missingLicense();
        }

        var licenseResult = conditionalRequests.execute(
                repository.id(),
                "license",
                "license for " + repository.fullName(),
                Instant.now(),
                (authorization, etag) -> client.getLicenseConditional(
                        repository.owner(),
                        repository.name(),
                        authorization,
                        GitHubInstallationTokenService.ACCEPT,
                        GitHubInstallationTokenService.API_VERSION,
                        etag),
                response -> response.readEntity(GitHubLicenseResponse.class),
                () -> null);

        if (licenseResult.reusedCached()) {
            return cachedComplete ? cachedLicense : refreshDirect(repository);
        }
        return toLicenseStatus(licenseResult.value());
    }

    private LicenseStatus refreshDirect(RepositorySummary repository) {
        List<GitHubContentItemResponse> rootContents = apiCalls.execute(
                "root contents for " + repository.fullName(),
                authorization -> client.getRootContents(
                        repository.owner(),
                        repository.name(),
                        authorization,
                        GitHubInstallationTokenService.ACCEPT,
                        GitHubInstallationTokenService.API_VERSION));
        List<GitHubContentItemResponse> contents =
                rootContents == null ? List.of() : rootContents;
        if (!containsLicenseFile(contents)) {
            return missingLicense();
        }

        try {
            GitHubLicenseResponse response = apiCalls.execute(
                    "license for " + repository.fullName(),
                    authorization -> client.getLicense(
                            repository.owner(),
                            repository.name(),
                            authorization,
                            GitHubInstallationTokenService.ACCEPT,
                            GitHubInstallationTokenService.API_VERSION));
            return toLicenseStatus(response);
        } catch (GitHubApiException exception) {
            if (exception.kind() == GitHubApiFailureKind.NOT_FOUND) {
                return customLicense();
            }
            throw exception;
        }
    }

    private RepositoryMetadataResult<LicenseStatus> fallback(
            LicenseStatus cachedLicense,
            boolean cachedComplete,
            RuntimeException exception) {
        String error = "license: " + safeMessage(exception);
        if (cachedComplete) {
            return RepositoryMetadataResult.degraded(cachedLicense, true, error);
        }
        return RepositoryMetadataResult.degraded(cachedLicense, false, error);
    }

    private boolean containsLicenseFile(List<GitHubContentItemResponse> contents) {
        return contents.stream()
                .filter(item -> item != null && "file".equalsIgnoreCase(item.type()))
                .map(GitHubContentItemResponse::name)
                .filter(name -> name != null)
                .anyMatch(this::isLicenseFileName);
    }

    private boolean isLicenseFileName(String name) {
        String normalized = name.trim().toUpperCase(Locale.ROOT);
        return normalized.equals("LICENSE")
                || normalized.startsWith("LICENSE.")
                || normalized.equals("LICENCE")
                || normalized.startsWith("LICENCE.");
    }

    private LicenseStatus missingLicense() {
        return new LicenseStatus(
                AnalysisState.COMPLETE,
                LicensePresence.MISSING,
                false,
                null,
                null);
    }

    private LicenseStatus customLicense() {
        return new LicenseStatus(
                AnalysisState.COMPLETE,
                LicensePresence.PRESENT,
                false,
                null,
                "Custom or unrecognized license");
    }

    private LicenseStatus toLicenseStatus(GitHubLicenseResponse response) {
        String key = response == null || response.license() == null
                ? null : response.license().key();
        String name = response == null || response.license() == null
                ? null : response.license().name();
        String spdxId = response == null || response.license() == null
                ? null : response.license().spdxId();
        return new LicenseStatus(
                AnalysisState.COMPLETE,
                LicensePresence.PRESENT,
                isRecognizedLicense(key, spdxId),
                key,
                name);
    }

    private boolean isRecognizedLicense(String key, String spdxId) {
        if (spdxId != null && !spdxId.isBlank() && !"NOASSERTION".equalsIgnoreCase(spdxId)) {
            return true;
        }
        return key != null && !key.isBlank() && !"other".equalsIgnoreCase(key);
    }

    private String safeMessage(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank()
                ? exception.getClass().getSimpleName()
                : message;
    }
}
