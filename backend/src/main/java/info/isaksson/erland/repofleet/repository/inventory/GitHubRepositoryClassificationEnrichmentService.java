package info.isaksson.erland.repofleet.repository.inventory;

import info.isaksson.erland.repofleet.github.auth.GitHubInstallationTokenService;
import info.isaksson.erland.repofleet.github.client.GitHubContentItemResponse;
import info.isaksson.erland.repofleet.github.client.GitHubLicenseResponse;
import info.isaksson.erland.repofleet.github.client.GitHubRepositoryMetadataClient;
import info.isaksson.erland.repofleet.github.client.GitHubTopicsResponse;
import info.isaksson.erland.repofleet.repository.api.AnalysisState;
import info.isaksson.erland.repofleet.repository.api.LicensePresence;
import info.isaksson.erland.repofleet.repository.api.LicenseStatus;
import info.isaksson.erland.repofleet.repository.api.RepositoryRefreshStatus;
import info.isaksson.erland.repofleet.repository.api.RepositorySummary;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
public class GitHubRepositoryClassificationEnrichmentService implements RepositoryEnrichmentService {

    private final GitHubInstallationTokenService tokenService;
    private final GitHubRepositoryMetadataClient client;

    @Inject
    public GitHubRepositoryClassificationEnrichmentService(
            GitHubInstallationTokenService tokenService,
            @RestClient GitHubRepositoryMetadataClient client) {
        this.tokenService = tokenService;
        this.client = client;
    }

    @Override
    public RepositorySummary enrich(RepositorySummary repository) {
        String authorization = "Bearer " + tokenService.getToken().value();

        List<String> topics = repository.topics();
        List<String> languages = repository.languages();
        String primaryLanguage = repository.primaryLanguage();
        LicenseStatus license = repository.license();

        boolean topicsComplete = false;
        boolean languagesComplete = false;
        boolean licenseComplete = false;
        List<String> errors = new ArrayList<>();

        try {
            GitHubTopicsResponse response = client.getTopics(
                    repository.owner(),
                    repository.name(),
                    authorization,
                    GitHubInstallationTokenService.ACCEPT,
                    GitHubInstallationTokenService.API_VERSION);
            topics = response == null ? List.of() : response.names().stream()
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();
            topicsComplete = true;
        } catch (RuntimeException exception) {
            errors.add("topics: " + safeMessage(exception));
        }

        try {
            Map<String, Long> response = client.getLanguages(
                    repository.owner(),
                    repository.name(),
                    authorization,
                    GitHubInstallationTokenService.ACCEPT,
                    GitHubInstallationTokenService.API_VERSION);
            Map<String, Long> languageBytes = response == null ? Map.of() : response;
            languages = languageBytes.keySet().stream()
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();
            primaryLanguage = languageBytes.entrySet().stream()
                    .max(Comparator.<Map.Entry<String, Long>>comparingLong(
                            entry -> entry.getValue() == null ? 0L : entry.getValue())
                            .thenComparing(Map.Entry::getKey, String.CASE_INSENSITIVE_ORDER))
                    .map(Map.Entry::getKey)
                    .orElse(null);
            languagesComplete = true;
        } catch (RuntimeException exception) {
            errors.add("languages: " + safeMessage(exception));
        }


        try {
            List<GitHubContentItemResponse> rootContents = client.getRootContents(
                    repository.owner(),
                    repository.name(),
                    authorization,
                    GitHubInstallationTokenService.ACCEPT,
                    GitHubInstallationTokenService.API_VERSION);
            List<GitHubContentItemResponse> contents = rootContents == null ? List.of() : rootContents;
            boolean licenseFilePresent = contents.stream()
                    .filter(item -> item != null && "file".equalsIgnoreCase(item.type()))
                    .map(GitHubContentItemResponse::name)
                    .filter(name -> name != null)
                    .anyMatch(this::isLicenseFileName);

            if (!licenseFilePresent) {
                license = new LicenseStatus(
                        AnalysisState.COMPLETE,
                        LicensePresence.MISSING,
                        false,
                        null,
                        null);
                licenseComplete = true;
            } else {
                try {
                    GitHubLicenseResponse response = client.getLicense(
                            repository.owner(),
                            repository.name(),
                            authorization,
                            GitHubInstallationTokenService.ACCEPT,
                            GitHubInstallationTokenService.API_VERSION);
                    String key = response == null || response.license() == null ? null : response.license().key();
                    String name = response == null || response.license() == null ? null : response.license().name();
                    String spdxId = response == null || response.license() == null ? null : response.license().spdxId();
                    boolean recognized = isRecognizedLicense(key, spdxId);
                    license = new LicenseStatus(
                            AnalysisState.COMPLETE,
                            LicensePresence.PRESENT,
                            recognized,
                            key,
                            name);
                    licenseComplete = true;
                } catch (jakarta.ws.rs.WebApplicationException exception) {
                    if (exception.getResponse() != null && exception.getResponse().getStatus() == 404) {
                        license = new LicenseStatus(
                                AnalysisState.COMPLETE,
                                LicensePresence.PRESENT,
                                false,
                                null,
                                "Custom or unrecognized license");
                        licenseComplete = true;
                    } else {
                        errors.add("license: " + safeMessage(exception));
                    }
                }
            }
        } catch (RuntimeException exception) {
            errors.add("license: " + safeMessage(exception));
        }

        AnalysisState state;
        String message;
        int completedAnalyses = (topicsComplete ? 1 : 0) + (languagesComplete ? 1 : 0) + (licenseComplete ? 1 : 0);
        if (completedAnalyses == 3) {
            state = AnalysisState.PARTIAL;
            message = "Topics, languages and LICENSE analyzed; remaining maintenance analyses pending.";
        } else if (completedAnalyses > 0) {
            state = AnalysisState.PARTIAL;
            message = "Repository enrichment partially completed (" + String.join("; ", errors) + ").";
        } else {
            state = AnalysisState.FAILED;
            message = "Repository enrichment failed (" + String.join("; ", errors) + ").";
        }

        return new RepositorySummary(
                repository.id(),
                repository.owner(),
                repository.name(),
                repository.fullName(),
                repository.url(),
                repository.visibility(),
                repository.archived(),
                repository.fork(),
                repository.defaultBranch(),
                topics,
                languages,
                primaryLanguage,
                license,
                repository.githubActions(),
                repository.release(),
                repository.activity(),
                new RepositoryRefreshStatus(state, message));
    }

    private boolean isLicenseFileName(String name) {
        String normalized = name.trim().toUpperCase(java.util.Locale.ROOT);
        return normalized.equals("LICENSE")
                || normalized.startsWith("LICENSE.")
                || normalized.equals("LICENCE")
                || normalized.startsWith("LICENCE.");
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
