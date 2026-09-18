package info.isaksson.erland.repofleet.github.conditional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.Optional;

@ApplicationScoped
public class GitHubConditionalRequestStateService {

    public Optional<GitHubConditionalRequestState> find(long githubRepositoryId, String resourceCategory) {
        return GitHubConditionalRequestState.find(
                "githubRepositoryId = ?1 and resourceCategory = ?2",
                githubRepositoryId,
                resourceCategory).firstResultOptional();
    }

    @Transactional
    public GitHubConditionalRequestState recordModified(
            long githubRepositoryId,
            String resourceCategory,
            String etag,
            Instant fetchedAt) {
        GitHubConditionalRequestState state = find(githubRepositoryId, resourceCategory)
                .orElseGet(GitHubConditionalRequestState::new);
        state.githubRepositoryId = githubRepositoryId;
        state.resourceCategory = resourceCategory;
        state.etag = etag;
        state.lastSuccessfulFetchAt = fetchedAt;
        state.updatedAt = fetchedAt;
        if (state.id == null) {
            state.persist();
        }
        return state;
    }

    @Transactional
    public GitHubConditionalRequestState recordNotModified(
            long githubRepositoryId,
            String resourceCategory,
            Instant fetchedAt) {
        GitHubConditionalRequestState state = find(githubRepositoryId, resourceCategory)
                .orElseThrow(() -> new IllegalStateException(
                        "304 received without cached conditional request state for "
                                + githubRepositoryId + "/" + resourceCategory));
        state.lastSuccessfulFetchAt = fetchedAt;
        state.updatedAt = fetchedAt;
        return state;
    }
}
