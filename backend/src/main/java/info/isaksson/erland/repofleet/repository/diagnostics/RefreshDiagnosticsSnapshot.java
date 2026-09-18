package info.isaksson.erland.repofleet.repository.diagnostics;

import java.time.Instant;
import java.util.List;

public record RefreshDiagnosticsSnapshot(
        Integer rateLimitRemaining,
        Instant rateLimitResetAt,
        long conditionalModifiedCount,
        long conditionalNotModifiedCount,
        long conditionalCachedFreshCount,
        long webhookTriggeredRefreshCount,
        long targetedFailedCount,
        List<RefreshRunDiagnostic> recentRuns,
        List<TargetedRefreshFailure> recentTargetedFailures) {

    public RefreshDiagnosticsSnapshot {
        recentRuns = recentRuns == null ? List.of() : List.copyOf(recentRuns);
        recentTargetedFailures = recentTargetedFailures == null
                ? List.of()
                : List.copyOf(recentTargetedFailures);
    }
}

record RefreshRunDiagnostic(
        long id,
        String triggerType,
        Instant startedAt,
        Instant completedAt,
        Long durationMillis,
        String finalState,
        int discoveredCount,
        int processedCount,
        int successfulCount,
        int errorCount,
        int reusedCount,
        int scheduledCount,
        String failureSummary) {
}

record TargetedRefreshFailure(
        long jobId,
        long githubRepositoryId,
        String triggerType,
        int attempts,
        String lastError,
        Instant completedAt) {
}
