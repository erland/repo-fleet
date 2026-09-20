package info.isaksson.erland.repofleet.repository.inventory;

import info.isaksson.erland.repofleet.github.api.GitHubApiCallExecutor;
import info.isaksson.erland.repofleet.github.auth.GitHubInstallationTokenService;
import info.isaksson.erland.repofleet.github.client.GitHubRepositoryMetadataClient;
import info.isaksson.erland.repofleet.github.client.GitHubWorkflowsResponse;
import info.isaksson.erland.repofleet.github.conditional.GitHubConditionalRequestExecutor;
import info.isaksson.erland.repofleet.repository.api.AnalysisState;
import info.isaksson.erland.repofleet.repository.api.GitHubActionsStatus;
import info.isaksson.erland.repofleet.repository.api.RepositorySummary;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Instant;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
class GitHubActionsEnrichmentComponent {

    private final GitHubRepositoryMetadataClient client;
    private final GitHubApiCallExecutor apiCalls;
    private final GitHubConditionalRequestExecutor conditionalRequests;

    @Inject
    GitHubActionsEnrichmentComponent(
            @RestClient GitHubRepositoryMetadataClient client,
            GitHubApiCallExecutor apiCalls,
            GitHubConditionalRequestExecutor conditionalRequests) {
        this.client = client;
        this.apiCalls = apiCalls;
        this.conditionalRequests = conditionalRequests;
    }

    GitHubActionsEnrichmentComponent(
            GitHubRepositoryMetadataClient client,
            GitHubApiCallExecutor apiCalls) {
        this(client, apiCalls, null);
    }

    RepositoryMetadataResult<GitHubActionsStatus> enrich(
            RepositorySummary repository,
            boolean cachedComplete) {
        GitHubActionsStatus cached = repository.githubActions();
        try {
            GitHubActionsStatus value;
            if (conditionalRequests == null) {
                value = refreshDirect(repository);
            } else {
                var result = conditionalRequests.execute(
                        repository.id(),
                        "workflows",
                        "workflows for " + repository.fullName(),
                        Instant.now(),
                        (authorization, etag) -> client.getWorkflowsConditional(
                                repository.owner(),
                                repository.name(),
                                authorization,
                                GitHubInstallationTokenService.ACCEPT,
                                GitHubInstallationTokenService.API_VERSION,
                                etag,
                                1,
                                1),
                        response -> response.readEntity(GitHubWorkflowsResponse.class),
                        () -> null);
                value = result.reusedCached()
                        ? cachedComplete ? cached : refreshDirect(repository)
                        : toStatus(result.value());
            }
            return RepositoryMetadataResult.success(value);
        } catch (RuntimeException exception) {
            String error = "actions: " + safeMessage(exception);
            if (cachedComplete) {
                return RepositoryMetadataResult.degraded(cached, true, error);
            }
            return RepositoryMetadataResult.degraded(
                    new GitHubActionsStatus(AnalysisState.FAILED, null, null),
                    false,
                    error);
        }
    }

    private GitHubActionsStatus refreshDirect(RepositorySummary repository) {
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
        return toStatus(response);
    }

    private GitHubActionsStatus toStatus(GitHubWorkflowsResponse response) {
        if (response == null) {
            throw new IllegalStateException("GitHub returned an empty workflows response.");
        }
        int workflowCount = Math.max(0, response.totalCount());
        return new GitHubActionsStatus(
                AnalysisState.COMPLETE,
                workflowCount > 0,
                workflowCount);
    }

    private String safeMessage(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank()
                ? exception.getClass().getSimpleName()
                : message;
    }
}
