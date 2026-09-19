package info.isaksson.erland.repofleet.repository.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import info.isaksson.erland.repofleet.repository.inventory.InventoryRefreshState;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class RepositoryRefreshHistoryServiceTest {

    @Inject
    RepositoryRefreshHistoryService history;

    @BeforeEach
    @Transactional
    void clearDatabase() {
        RepositoryRefreshRun.deleteAll();
    }

    @Test
    void persistsAndReturnsCompletedRefreshRun() {
        Instant startedAt = Instant.parse("2026-09-18T08:00:00Z");
        Instant completedAt = Instant.parse("2026-09-18T08:02:00Z");

        long id = history.startRun("MANUAL", startedAt);
        history.completeRun(
                id,
                InventoryRefreshState.PARTIAL,
                completedAt,
                10,
                10,
                8,
                2,
                6,
                4,
                "2 repository enrichment(s) completed with errors.");

        var run = history.recentRuns(10).getFirst();

        assertEquals(id, run.id());
        assertEquals("MANUAL", run.triggerType());
        assertEquals(startedAt, run.startedAt());
        assertEquals(completedAt, run.completedAt());
        assertEquals("PARTIAL", run.finalState());
        assertEquals(10, run.discoveredCount());
        assertEquals(10, run.processedCount());
        assertEquals(8, run.successfulCount());
        assertEquals(2, run.errorCount());
        assertEquals(6, run.reusedCount());
        assertEquals(4, run.scheduledCount());
        assertNotNull(run.failedRepositorySummary());
        assertEquals(startedAt, history.latestAttemptAt());
    }
}
