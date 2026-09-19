package info.isaksson.erland.repofleet.repository.diagnostics;

import info.isaksson.erland.repofleet.github.diagnostics.GitHubApiDiagnosticsService;
import info.isaksson.erland.repofleet.github.api.GitHubRateLimitWaitState;
import info.isaksson.erland.repofleet.repository.persistence.RepositoryRefreshHistoryService;
import info.isaksson.erland.repofleet.repository.refresh.RepositoryRefreshJob;
import info.isaksson.erland.repofleet.repository.refresh.RepositoryRefreshJobState;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@ApplicationScoped
public class RefreshDiagnosticsService {

    private final GitHubApiDiagnosticsService githubApi;
    private final RepositoryRefreshHistoryService history;
    private final GitHubRateLimitWaitState rateLimitWaitState;

    @Inject
    public RefreshDiagnosticsService(
            GitHubApiDiagnosticsService githubApi,
            RepositoryRefreshHistoryService history,
            GitHubRateLimitWaitState rateLimitWaitState) {
        this.githubApi = githubApi;
        this.history = history;
        this.rateLimitWaitState = rateLimitWaitState;
    }

    @Transactional
    public RefreshDiagnosticsSnapshot snapshot() {
        var api = githubApi.snapshot();
        Instant now = Instant.now();
        rateLimitWaitState.clearIfElapsed(now);

        List<RefreshRunDiagnostic> runs = history.recentRuns(10).stream()
                .map(run -> new RefreshRunDiagnostic(
                        run.id(),
                        run.triggerType(),
                        run.startedAt(),
                        run.completedAt(),
                        run.completedAt() == null
                                ? null
                                : Duration.between(run.startedAt(), run.completedAt()).toMillis(),
                        run.finalState(),
                        run.discoveredCount(),
                        run.processedCount(),
                        run.successfulCount(),
                        run.errorCount(),
                        run.reusedCount(),
                        run.scheduledCount(),
                        run.failedRepositorySummary()))
                .toList();

        long webhookTriggered = RepositoryRefreshJob.count("triggerType like ?1", "WEBHOOK_%");
        long failedCount = RepositoryRefreshJob.count("state", RepositoryRefreshJobState.FAILED);

        List<TargetedRefreshFailure> failures = RepositoryRefreshJob.find(
                        "state = ?1 order by completedAt desc",
                        RepositoryRefreshJobState.FAILED)
                .page(0, 10)
                .list()
                .stream()
                .map(RepositoryRefreshJob.class::cast)
                .map(job -> new TargetedRefreshFailure(
                        job.id,
                        job.githubRepositoryId,
                        job.triggerType,
                        job.attempts,
                        job.lastError,
                        job.completedAt))
                .toList();

        return new RefreshDiagnosticsSnapshot(
                api.rateLimitRemaining,
                api.rateLimitResetAt,
                rateLimitWaitState.paused(now),
                rateLimitWaitState.pausedUntil(),
                rateLimitWaitState.reason(),
                api.conditionalModifiedCount,
                api.conditionalNotModifiedCount,
                api.conditionalCachedFreshCount,
                webhookTriggered,
                failedCount,
                runs,
                failures);
    }
}
