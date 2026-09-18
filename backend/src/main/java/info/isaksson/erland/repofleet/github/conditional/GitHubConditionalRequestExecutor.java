package info.isaksson.erland.repofleet.github.conditional;

import info.isaksson.erland.repofleet.github.api.GitHubApiCallExecutor;
import info.isaksson.erland.repofleet.github.diagnostics.GitHubApiDiagnosticsService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import info.isaksson.erland.repofleet.repository.refresh.RepositoryRefreshPolicy;
import java.time.Instant;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

@ApplicationScoped
public class GitHubConditionalRequestExecutor {

    private final GitHubApiCallExecutor apiCalls;
    private final GitHubConditionalRequestStateService stateService;
    private final RepositoryRefreshPolicy refreshPolicy;
    private final GitHubApiDiagnosticsService diagnostics;

    @Inject
    public GitHubConditionalRequestExecutor(
            GitHubApiCallExecutor apiCalls,
            GitHubConditionalRequestStateService stateService,
            RepositoryRefreshPolicy refreshPolicy,
            GitHubApiDiagnosticsService diagnostics) {
        this.apiCalls = apiCalls;
        this.stateService = stateService;
        this.refreshPolicy = refreshPolicy;
        this.diagnostics = diagnostics;
    }

    GitHubConditionalRequestExecutor(
            GitHubApiCallExecutor apiCalls,
            GitHubConditionalRequestStateService stateService) {
        this(apiCalls, stateService, null, null);
    }

    public <T> GitHubConditionalResult<T> execute(
            long githubRepositoryId,
            String resourceCategory,
            String operation,
            Instant fetchedAt,
            BiFunction<String, String, Response> request,
            Function<Response, T> bodyReader,
            Supplier<T> cachedValue) {

        var previousState = stateService.find(githubRepositoryId, resourceCategory);
        String previousEtag = previousState.map(state -> state.etag).orElse(null);

        if (refreshPolicy != null
                && previousState.isPresent()
                && refreshPolicy.categoryFresh(
                        resourceCategory,
                        previousState.get().lastSuccessfulFetchAt,
                        fetchedAt)) {
            GitHubConditionalResult<T> result =
                    GitHubConditionalResult.cachedFresh(cachedValue.get(), previousEtag);
            if (diagnostics != null) {
                diagnostics.recordConditional(result.status(), null, fetchedAt);
            }
            return result;
        }

        Response response = apiCalls.execute(
                operation,
                authorization -> {
                    Response current = request.apply(authorization, previousEtag);
                    int status = current.getStatus();
                    if (status == 304 || (status >= 200 && status < 300)) {
                        return current;
                    }
                    throw new WebApplicationException(current);
                });

        try (response) {
            if (response.getStatus() == 304) {
                var state = stateService.recordNotModified(
                        githubRepositoryId,
                        resourceCategory,
                        fetchedAt);
                GitHubConditionalResult<T> result =
                        GitHubConditionalResult.notModified(cachedValue.get(), state.etag);
                if (diagnostics != null) {
                    diagnostics.recordConditional(result.status(), response, fetchedAt);
                }
                return result;
            }

            T value = bodyReader.apply(response);
            String etag = response.getHeaderString("ETag");
            stateService.recordModified(
                    githubRepositoryId,
                    resourceCategory,
                    etag,
                    fetchedAt);
            GitHubConditionalResult<T> result = GitHubConditionalResult.modified(value, etag);
            if (diagnostics != null) {
                diagnostics.recordConditional(result.status(), response, fetchedAt);
            }
            return result;
        }
    }
}
