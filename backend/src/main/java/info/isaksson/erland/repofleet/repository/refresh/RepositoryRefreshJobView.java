package info.isaksson.erland.repofleet.repository.refresh;

import java.time.Instant;

public record RepositoryRefreshJobView(
        long id,
        long githubRepositoryId,
        String triggerType,
        RepositoryRefreshJobState state,
        int attempts,
        int maxAttempts,
        Instant nextAttemptAt,
        String lastError,
        Instant createdAt,
        Instant updatedAt,
        Instant completedAt) {
}
