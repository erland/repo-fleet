package info.isaksson.erland.repofleet.repository.refresh;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import info.isaksson.erland.repofleet.repository.api.RepositoryVisibility;
import info.isaksson.erland.repofleet.repository.persistence.RepositoryIdentityRepository;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class RepositoryRefreshQueueServiceTest {

    @Inject
    RepositoryRefreshQueueService queue;

    @Inject
    RepositoryIdentityRepository identities;

    @BeforeEach
    @Transactional
    void clearDatabase() {
        RepositoryRefreshJob.deleteAll();
        identities.deleteAll();
    }

    @Test
    @Transactional
    void deduplicatesActiveJobsAndRecoversInterruptedWork() {
        Instant now = Instant.parse("2026-09-18T15:00:00Z");
        persistRepository(501L, now);

        var first = queue.enqueue(501L, "WEBHOOK_PUSH", now);
        var duplicate = queue.enqueue(501L, "MANUAL_SINGLE_REPOSITORY", now.plusSeconds(1));

        assertEquals(first.id(), duplicate.id());
        assertEquals(1L, RepositoryRefreshJob.count());

        var claimed = queue.claimNext(now.plusSeconds(2));
        assertEquals(RepositoryRefreshJobState.RUNNING, claimed.state());

        queue.recoverInterrupted(now.plusSeconds(3));
        var recovered = queue.get(first.id());
        assertEquals(RepositoryRefreshJobState.RETRY, recovered.state());

        var retried = queue.claimNext(now.plusSeconds(3));
        assertEquals(first.id(), retried.id());
        assertEquals(2, retried.attempts());

        queue.complete(first.id(), now.plusSeconds(4));
        assertEquals(RepositoryRefreshJobState.SUCCEEDED, queue.get(first.id()).state());

        var next = queue.enqueue(501L, "MANUAL_SINGLE_REPOSITORY", now.plusSeconds(5));
        assertNotEquals(first.id(), next.id());
    }

    @Test
    @Transactional
    void retriesWithBackoffThenFailsAtMaximumAttempts() {
        Instant now = Instant.parse("2026-09-18T15:00:00Z");
        persistRepository(502L, now);

        var job = queue.enqueue(502L, "WEBHOOK_RELEASES", now);

        var attempt1 = queue.claimNext(now);
        queue.fail(attempt1.id(), "first", now);
        var retry1 = queue.get(job.id());
        assertEquals(RepositoryRefreshJobState.RETRY, retry1.state());

        var attempt2 = queue.claimNext(retry1.nextAttemptAt());
        queue.fail(attempt2.id(), "second", retry1.nextAttemptAt());
        var retry2 = queue.get(job.id());
        assertEquals(RepositoryRefreshJobState.RETRY, retry2.state());

        var attempt3 = queue.claimNext(retry2.nextAttemptAt());
        queue.fail(attempt3.id(), "third", retry2.nextAttemptAt());

        var failed = queue.get(job.id());
        assertEquals(RepositoryRefreshJobState.FAILED, failed.state());
        assertEquals(3, failed.attempts());
        assertEquals("third", failed.lastError());
        assertNotEquals(null, failed.completedAt());
    }

    @Test
    void returnsNullWhenNoJobIsDue() {
        assertNull(queue.claimNext(Instant.parse("2026-09-18T15:00:00Z")));
    }

    private void persistRepository(long id, Instant now) {
        identities.insert(
                id,
                "erland",
                "repo-" + id,
                "erland/repo-" + id,
                RepositoryVisibility.PRIVATE,
                false,
                false,
                "main",
                now,
                now,
                now);
    }
}
