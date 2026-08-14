package info.isaksson.erland.repofleet.repository.inventory;

import info.isaksson.erland.repofleet.github.auth.GitHubInstallationTokenService;
import info.isaksson.erland.repofleet.github.client.GitHubRepositoryMetadataClient;
import info.isaksson.erland.repofleet.github.client.GitHubTopicsResponse;
import info.isaksson.erland.repofleet.repository.api.AnalysisState;
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

        boolean topicsComplete = false;
        boolean languagesComplete = false;
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

        AnalysisState state;
        String message;
        if (topicsComplete && languagesComplete) {
            // Classification is complete; other Phase 1 analyses are intentionally still pending.
            state = AnalysisState.PARTIAL;
            message = "Topics and languages enriched; remaining maintenance analyses pending.";
        } else if (topicsComplete || languagesComplete) {
            state = AnalysisState.PARTIAL;
            message = "Repository classification partially enriched (" + String.join("; ", errors) + ").";
        } else {
            state = AnalysisState.FAILED;
            message = "Repository classification enrichment failed (" + String.join("; ", errors) + ").";
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
                repository.license(),
                repository.githubActions(),
                repository.release(),
                repository.activity(),
                new RepositoryRefreshStatus(state, message));
    }

    private String safeMessage(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank()
                ? exception.getClass().getSimpleName()
                : message;
    }
}
