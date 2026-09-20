package info.isaksson.erland.repofleet.repository.inventory;

import info.isaksson.erland.repofleet.github.api.GitHubApiCallExecutor;
import info.isaksson.erland.repofleet.github.auth.GitHubInstallationTokenService;
import info.isaksson.erland.repofleet.github.client.GitHubRepositoryMetadataClient;
import info.isaksson.erland.repofleet.github.conditional.GitHubConditionalRequestExecutor;
import info.isaksson.erland.repofleet.repository.api.RepositorySummary;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.GenericType;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
class GitHubLanguagesEnrichmentComponent {

    private final GitHubRepositoryMetadataClient client;
    private final GitHubApiCallExecutor apiCalls;
    private final GitHubConditionalRequestExecutor conditionalRequests;

    @Inject
    GitHubLanguagesEnrichmentComponent(
            @RestClient GitHubRepositoryMetadataClient client,
            GitHubApiCallExecutor apiCalls,
            GitHubConditionalRequestExecutor conditionalRequests) {
        this.client = client;
        this.apiCalls = apiCalls;
        this.conditionalRequests = conditionalRequests;
    }

    GitHubLanguagesEnrichmentComponent(
            GitHubRepositoryMetadataClient client,
            GitHubApiCallExecutor apiCalls) {
        this(client, apiCalls, null);
    }

    RepositoryMetadataResult<RepositoryLanguageMetadata> enrich(
            RepositorySummary repository,
            boolean cachedComplete,
            boolean allowFailureFallback) {
        RepositoryLanguageMetadata cached =
                new RepositoryLanguageMetadata(repository.languages(), repository.primaryLanguage());
        try {
            RepositoryLanguageMetadata metadata;
            if (conditionalRequests == null) {
                Map<String, Long> response = apiCalls.execute(
                        "languages for " + repository.fullName(),
                        authorization -> client.getLanguages(
                                repository.owner(),
                                repository.name(),
                                authorization,
                                GitHubInstallationTokenService.ACCEPT,
                                GitHubInstallationTokenService.API_VERSION));
                metadata = languageMetadata(response);
            } else {
                var result = conditionalRequests.execute(
                        repository.id(),
                        "languages",
                        "languages for " + repository.fullName(),
                        Instant.now(),
                        (authorization, etag) -> client.getLanguagesConditional(
                                repository.owner(),
                                repository.name(),
                                authorization,
                                GitHubInstallationTokenService.ACCEPT,
                                GitHubInstallationTokenService.API_VERSION,
                                etag),
                        response -> languageMetadata(
                                response.readEntity(new GenericType<Map<String, Long>>() {})),
                        () -> cached);
                metadata = result.value();
            }
            return RepositoryMetadataResult.success(metadata);
        } catch (RuntimeException exception) {
            String error = "languages: " + safeMessage(exception);
            if (allowFailureFallback && cachedComplete) {
                return RepositoryMetadataResult.degraded(cached, true, error);
            }
            return RepositoryMetadataResult.degraded(cached, false, error);
        }
    }

    private RepositoryLanguageMetadata languageMetadata(Map<String, Long> response) {
        Map<String, Long> languageBytes = response == null ? Map.of() : response;
        List<String> languageNames = languageBytes.keySet().stream()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
        String primary = languageBytes.entrySet().stream()
                .max(Comparator.<Map.Entry<String, Long>>comparingLong(
                        entry -> entry.getValue() == null ? 0L : entry.getValue())
                        .thenComparing(Map.Entry::getKey, String.CASE_INSENSITIVE_ORDER))
                .map(Map.Entry::getKey)
                .orElse(null);
        return new RepositoryLanguageMetadata(languageNames, primary);
    }

    private String safeMessage(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank()
                ? exception.getClass().getSimpleName()
                : message;
    }
}
