package info.isaksson.erland.repofleet.repository.persistence;

import info.isaksson.erland.repofleet.repository.inventory.InventoryRefreshState;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.List;

@ApplicationScoped
public class RepositoryRefreshHistoryService {

    @Transactional
    public long startRun(String triggerType, Instant startedAt) {
        RepositoryRefreshRun run = new RepositoryRefreshRun();
        run.triggerType = triggerType;
        run.startedAt = startedAt;
        run.finalState = InventoryRefreshState.RUNNING.name();
        run.persist();
        return run.id;
    }

    @Transactional
    public void completeRun(
            long runId,
            InventoryRefreshState state,
            Instant completedAt,
            int discoveredCount,
            int processedCount,
            int successfulCount,
            int errorCount,
            int reusedCount,
            int scheduledCount,
            String failedRepositorySummary) {
        RepositoryRefreshRun run = RepositoryRefreshRun.findById(runId);
        if (run == null) {
            throw new IllegalArgumentException("Unknown refresh run ID: " + runId);
        }
        run.completedAt = completedAt;
        run.finalState = state.name();
        run.discoveredCount = discoveredCount;
        run.processedCount = processedCount;
        run.successfulCount = successfulCount;
        run.errorCount = errorCount;
        run.reusedCount = reusedCount;
        run.scheduledCount = scheduledCount;
        run.failedRepositorySummary = failedRepositorySummary;
    }

    @Transactional
    public List<RepositoryRefreshRunSummary> recentRuns(int limit) {
        int boundedLimit = Math.max(1, Math.min(limit, 100));
        return RepositoryRefreshRun.find("order by startedAt desc")
                .page(0, boundedLimit)
                .list()
                .stream()
                .map(entity -> {
                    RepositoryRefreshRun run = (RepositoryRefreshRun) entity;
                    return new RepositoryRefreshRunSummary(
                            run.id,
                            run.triggerType,
                            run.startedAt,
                            run.completedAt,
                            run.finalState,
                            run.discoveredCount,
                            run.processedCount,
                            run.successfulCount,
                            run.errorCount,
                            run.reusedCount,
                            run.scheduledCount,
                            run.failedRepositorySummary,
                            run.rateLimitRemaining,
                            run.rateLimitResetAt);
                })
                .toList();
    }
}
