package info.isaksson.erland.repofleet.repository.inventory;

import info.isaksson.erland.repofleet.github.api.GitHubApiCallExecutor;
import info.isaksson.erland.repofleet.github.api.GitHubApiException;
import info.isaksson.erland.repofleet.github.api.GitHubApiFailureKind;
import info.isaksson.erland.repofleet.github.auth.GitHubInstallationTokenService;
import info.isaksson.erland.repofleet.github.client.GitHubContentItemResponse;
import info.isaksson.erland.repofleet.github.client.GitHubLicenseResponse;
import info.isaksson.erland.repofleet.github.client.GitHubReleaseResponse;
import info.isaksson.erland.repofleet.github.client.GitHubRepositoryMetadataClient;
import info.isaksson.erland.repofleet.github.client.GitHubTopicsResponse;
import info.isaksson.erland.repofleet.github.client.GitHubWorkflowsResponse;
import info.isaksson.erland.repofleet.repository.api.AnalysisState;
import info.isaksson.erland.repofleet.repository.api.GitHubActionsStatus;
import info.isaksson.erland.repofleet.repository.api.LicensePresence;
import info.isaksson.erland.repofleet.repository.api.LicenseStatus;
import info.isaksson.erland.repofleet.repository.api.ReleaseStatus;
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
    private final GitHubApiCallExecutor apiCalls;

    @Inject
    public GitHubRepositoryClassificationEnrichmentService(
            GitHubInstallationTokenService tokenService,
            @RestClient GitHubRepositoryMetadataClient client,
            GitHubApiCallExecutor apiCalls) {
        this.tokenService = tokenService;
        this.client = client;
        this.apiCalls = apiCalls;
    }

    GitHubRepositoryClassificationEnrichmentService(
            GitHubInstallationTokenService tokenService,
            GitHubRepositoryMetadataClient client) {
        this(tokenService, client, new GitHubApiCallExecutor(tokenService));
    }

    @Override
    public RepositorySummary enrich(RepositorySummary repository) {
        List<String> topics = repository.topics();
        List<String> languages = repository.languages();
        String primaryLanguage = repository.primaryLanguage();
        LicenseStatus license = repository.license();
        GitHubActionsStatus githubActions = repository.githubActions();
        ReleaseStatus release = repository.release();

        boolean topicsComplete = false;
        boolean languagesComplete = false;
        boolean licenseComplete = false;
        boolean actionsComplete = false;
        boolean releaseComplete = false;
        List<String> errors = new ArrayList<>();

        try {
            GitHubTopicsResponse response = apiCalls.execute(
                    "topics for " + repository.fullName(),
                    authorization -> client.getTopics(
                            repository.owner(),
                            repository.name(),
                            authorization,
                            GitHubInstallationTokenService.ACCEPT,
                            GitHubInstallationTokenService.API_VERSION));
            topics = response == null ? List.of() : response.names().stream()
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();
            topicsComplete = true;
        } catch (GitHubApiException exception) {
            if (exception.kind() == GitHubApiFailureKind.NOT_FOUND) {
                return unavailableRepository(repository, exception);
            }
            errors.add("topics: " + safeMessage(exception));
        } catch (RuntimeException exception) {
            errors.add("topics: " + safeMessage(exception));
        }

        try {
            Map<String, Long> response = apiCalls.execute(
                    "languages for " + repository.fullName(),
                    authorization -> client.getLanguages(
                            repository.owner(),
                            repository.name(),
                            authorization,
                            GitHubInstallationTokenService.ACCEPT,
                            GitHubInstallationTokenService.API_VERSION));
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
            List<GitHubContentItemResponse> rootContents = apiCalls.execute(
                    "root contents for " + repository.fullName(),
                    authorization -> client.getRootContents(
                            repository.owner(),
                            repository.name(),
                            authorization,
                            GitHubInstallationTokenService.ACCEPT,
                            GitHubInstallationTokenService.API_VERSION));
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
                    GitHubLicenseResponse response = apiCalls.execute(
                            "license for " + repository.fullName(),
                            authorization -> client.getLicense(
                                    repository.owner(),
                                    repository.name(),
                                    authorization,
                                    GitHubInstallationTokenService.ACCEPT,
                                    GitHubInstallationTokenService.API_VERSION));
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
                } catch (GitHubApiException exception) {
                    if (exception.kind() == GitHubApiFailureKind.NOT_FOUND) {
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


        try {
            GitHubWorkflowsResponse response = apiCalls.execute(
                    "workflows for " + repository.fullName(),
                    authorization -> client.getWorkflows(
                            repository.owner(),
                            repository.name(),
                            authorization,
                            GitHubInstallationTokenService.ACCEPT,
                            GitHubInstallationTokenService.API_VERSION,
                            1,
                            1));
            if (response == null) {
                throw new IllegalStateException("GitHub returned an empty workflows response.");
            }
            int workflowCount = Math.max(0, response.totalCount());
            githubActions = new GitHubActionsStatus(
                    AnalysisState.COMPLETE,
                    workflowCount > 0,
                    workflowCount);
            actionsComplete = true;
        } catch (RuntimeException exception) {
            githubActions = new GitHubActionsStatus(
                    AnalysisState.FAILED,
                    null,
                    null);
            errors.add("actions: " + safeMessage(exception));
        }


        try {
            GitHubReleaseResponse latest = findLatestPublishedRelease(repository);
            release = latest == null
                    ? new ReleaseStatus(AnalysisState.COMPLETE, false, null, null, null, null)
                    : new ReleaseStatus(
                            AnalysisState.COMPLETE,
                            true,
                            latest.name(),
                            latest.tagName(),
                            latest.publishedAt() != null ? latest.publishedAt() : latest.createdAt(),
                            latest.prerelease());
            releaseComplete = true;
        } catch (RuntimeException exception) {
            release = new ReleaseStatus(
                    AnalysisState.FAILED,
                    null,
                    null,
                    null,
                    null,
                    null);
            errors.add("release: " + safeMessage(exception));
        }

        AnalysisState state;
        String message;
        int completedAnalyses = (topicsComplete ? 1 : 0)
                + (languagesComplete ? 1 : 0)
                + (licenseComplete ? 1 : 0)
                + (actionsComplete ? 1 : 0)
                + (releaseComplete ? 1 : 0);
        if (completedAnalyses == 5) {
            state = AnalysisState.COMPLETE;
            message = "Repository enrichment complete.";
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
                githubActions,
                release,
                repository.activity(),
                new RepositoryRefreshStatus(state, message));
    }

    private RepositorySummary unavailableRepository(
            RepositorySummary repository,
            GitHubApiException exception) {
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
                repository.topics(),
                repository.languages(),
                repository.primaryLanguage(),
                repository.license(),
                repository.githubActions(),
                repository.release(),
                repository.activity(),
                new RepositoryRefreshStatus(
                        AnalysisState.FAILED,
                        "Repository became unavailable during refresh: " + safeMessage(exception)));
    }

    private java.time.Instant releaseTimestamp(GitHubReleaseResponse release) {
        return release.publishedAt() != null ? release.publishedAt() : release.createdAt();
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

    private GitHubReleaseResponse findLatestPublishedRelease(RepositorySummary repository) {
        int page = 1;
        while (true) {
            int requestedPage = page;
            List<GitHubReleaseResponse> response = apiCalls.execute(
                    "releases for " + repository.fullName() + " page " + requestedPage,
                    authorization -> client.getReleases(
                            repository.owner(),
                            repository.name(),
                            authorization,
                            GitHubInstallationTokenService.ACCEPT,
                            GitHubInstallationTokenService.API_VERSION,
                            100,
                            requestedPage));
            List<GitHubReleaseResponse> releases = response == null ? List.of() : response;
            GitHubReleaseResponse latest = releases.stream()
                    .filter(item -> item != null && !item.draft())
                    .max(Comparator.comparing(
                            this::releaseTimestamp,
                            Comparator.nullsFirst(Comparator.naturalOrder())))
                    .orElse(null);
            if (latest != null || releases.size() < 100) {
                return latest;
            }
            page++;
        }
    }

    private String safeMessage(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank()
                ? exception.getClass().getSimpleName()
                : message;
    }
}
