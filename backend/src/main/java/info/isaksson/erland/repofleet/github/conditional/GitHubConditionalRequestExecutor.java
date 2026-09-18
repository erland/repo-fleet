package info.isaksson.erland.repofleet.github.conditional;

import info.isaksson.erland.repofleet.github.api.GitHubApiCallExecutor;
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

    @Inject
    public GitHubConditionalRequestExecutor(
            GitHubApiCallExecutor apiCalls,
            GitHubConditionalRequestStateService stateService,
            RepositoryRefreshPolicy refreshPolicy) {
        this.apiCalls = apiCalls;
        this.stateService = stateService;
        this.refreshPolicy = refreshPolicy;
    }

    GitHubConditionalRequestExecutor(
            GitHubApiCallExecutor apiCalls,
            GitHubConditionalRequestStateService stateService) {
        this(apiCalls, stateService, null);
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
            return GitHubConditionalResult.cachedFresh(cachedValue.get(), previousEtag);
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
                return GitHubConditionalResult.notModified(cachedValue.get(), state.etag);
            }

            T value = bodyReader.apply(response);
            String etag = response.getHeaderString("ETag");
            stateService.recordModified(
                    githubRepositoryId,
                    resourceCategory,
                    etag,
                    fetchedAt);
            return GitHubConditionalResult.modified(value, etag);
        }
    }
}
