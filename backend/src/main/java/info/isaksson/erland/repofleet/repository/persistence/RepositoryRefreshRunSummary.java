package info.isaksson.erland.repofleet.repository.persistence;

import java.time.Instant;

public record RepositoryRefreshRunSummary(
        long id,
        String triggerType,
        Instant startedAt,
        Instant completedAt,
        String finalState,
        int discoveredCount,
        int processedCount,
        int successfulCount,
        int errorCount,
        int reusedCount,
        int scheduledCount,
        String failedRepositorySummary,
        Integer rateLimitRemaining,
        Instant rateLimitResetAt) {
}
