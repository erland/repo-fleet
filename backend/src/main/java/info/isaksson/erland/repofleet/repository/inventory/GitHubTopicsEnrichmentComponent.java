package info.isaksson.erland.repofleet.repository.inventory;

import info.isaksson.erland.repofleet.github.api.GitHubApiCallExecutor;
import info.isaksson.erland.repofleet.github.api.GitHubApiException;
import info.isaksson.erland.repofleet.github.api.GitHubApiFailureKind;
import info.isaksson.erland.repofleet.github.auth.GitHubInstallationTokenService;
import info.isaksson.erland.repofleet.github.client.GitHubRepositoryMetadataClient;
import info.isaksson.erland.repofleet.github.client.GitHubTopicsResponse;
import info.isaksson.erland.repofleet.github.conditional.GitHubConditionalRequestExecutor;
import info.isaksson.erland.repofleet.repository.api.RepositorySummary;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.List;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
class GitHubTopicsEnrichmentComponent {

    private final GitHubRepositoryMetadataClient client;
    private final GitHubApiCallExecutor apiCalls;
    private final GitHubConditionalRequestExecutor conditionalRequests;

    @Inject
    GitHubTopicsEnrichmentComponent(
            @RestClient GitHubRepositoryMetadataClient client,
            GitHubApiCallExecutor apiCalls,
            GitHubConditionalRequestExecutor conditionalRequests) {
        this.client = client;
        this.apiCalls = apiCalls;
        this.conditionalRequests = conditionalRequests;
    }

    GitHubTopicsEnrichmentComponent(
            GitHubRepositoryMetadataClient client,
            GitHubApiCallExecutor apiCalls) {
        this(client, apiCalls, null);
    }

    RepositoryMetadataResult<List<String>> enrich(
            RepositorySummary repository,
            boolean cachedComplete,
            boolean allowFailureFallback) {
        List<String> cachedTopics = repository.topics();
        try {
            List<String> topics;
            if (conditionalRequests == null) {
                GitHubTopicsResponse response = apiCalls.execute(
                        "topics for " + repository.fullName(),
                        authorization -> client.getTopics(
                                repository.owner(),
                                repository.name(),
                                authorization,
                                GitHubInstallationTokenService.ACCEPT,
                                GitHubInstallationTokenService.API_VERSION));
                topics = normalizedTopics(response);
            } else {
                var result = conditionalRequests.execute(
                        repository.id(),
                        "topics",
                        "topics for " + repository.fullName(),
                        Instant.now(),
                        (authorization, etag) -> client.getTopicsConditional(
                                repository.owner(),
                                repository.name(),
                                authorization,
                                GitHubInstallationTokenService.ACCEPT,
                                GitHubInstallationTokenService.API_VERSION,
                                etag),
                        response -> normalizedTopics(response.readEntity(GitHubTopicsResponse.class)),
                        () -> cachedTopics);
                topics = result.value();
            }
            return RepositoryMetadataResult.success(topics);
        } catch (GitHubApiException exception) {
            String error = "topics: " + safeMessage(exception);
            if (exception.kind() == GitHubApiFailureKind.NOT_FOUND) {
                return RepositoryMetadataResult.unavailable(cachedTopics, error);
            }
            if (allowFailureFallback && cachedComplete) {
                return RepositoryMetadataResult.degraded(cachedTopics, true, error);
            }
            return RepositoryMetadataResult.degraded(cachedTopics, false, error);
        } catch (RuntimeException exception) {
            String error = "topics: " + safeMessage(exception);
            if (allowFailureFallback && cachedComplete) {
                return RepositoryMetadataResult.degraded(cachedTopics, true, error);
            }
            return RepositoryMetadataResult.degraded(cachedTopics, false, error);
        }
    }

    private List<String> normalizedTopics(GitHubTopicsResponse response) {
        return response == null ? List.of() : response.names().stream()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    private String safeMessage(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank()
                ? exception.getClass().getSimpleName()
                : message;
    }
}
