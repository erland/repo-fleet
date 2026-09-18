package info.isaksson.erland.repofleet.repository.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import info.isaksson.erland.repofleet.repository.api.ActivityStatus;
import info.isaksson.erland.repofleet.repository.api.AnalysisState;
import info.isaksson.erland.repofleet.repository.api.GitHubActionsStatus;
import info.isaksson.erland.repofleet.repository.api.LicensePresence;
import info.isaksson.erland.repofleet.repository.api.LicenseStatus;
import info.isaksson.erland.repofleet.repository.api.ReleaseStatus;
import info.isaksson.erland.repofleet.repository.api.RepositoryRefreshStatus;
import info.isaksson.erland.repofleet.repository.api.RepositorySummary;
import info.isaksson.erland.repofleet.repository.api.RepositoryVisibility;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class RepositoryInventoryPersistenceServiceTest {

    @Inject
    RepositoryInventoryPersistenceService persistenceService;

    @Inject
    RepositoryIdentityRepository repository;

    @BeforeEach
    @Transactional
    void clearDatabase() {
        repository.deleteAll();
    }

    @Test
    @Transactional
    void insertsNewlyDiscoveredRepositories() {
        Instant seenAt = Instant.parse("2026-09-18T08:00:00Z");

        persistenceService.synchronize(
                List.of(repositorySummary(1001L, "one"), repositorySummary(1002L, "two")),
                seenAt);

        assertEquals(2, repository.count());
        RepositoryIdentity stored = repository.findByGitHubRepositoryId(1001L).orElseThrow();
        assertEquals("erland/one", stored.fullName);
        assertEquals(Instant.parse("2026-09-17T12:05:00Z"), stored.githubUpdatedAt);
        assertEquals(Instant.parse("2026-09-17T12:00:00Z"), stored.githubPushedAt);
        assertEquals(seenAt, stored.firstSeenAt);
        assertEquals(seenAt, stored.lastSeenAt);
        assertTrue(stored.active);
    }

    @Test
    @Transactional
    void updatesExistingRepositoryAndPreservesFirstSeen() {
        Instant firstSeen = Instant.parse("2026-09-18T07:00:00Z");
        Instant secondSeen = Instant.parse("2026-09-18T08:00:00Z");

        persistenceService.synchronize(List.of(repositorySummary(1001L, "one")), firstSeen);

        RepositorySummary renamed = repositorySummary(1001L, "renamed");
        persistenceService.synchronize(List.of(renamed), secondSeen);

        assertEquals(1, repository.count());
        RepositoryIdentity stored = repository.findByGitHubRepositoryId(1001L).orElseThrow();
        assertEquals("erland/renamed", stored.fullName);
        assertEquals(firstSeen, stored.firstSeenAt);
        assertEquals(secondSeen, stored.lastSeenAt);
        assertTrue(stored.active);
    }

    @Test
    @Transactional
    void marksRepositoriesMissingFromSuccessfulDiscoveryInactive() {
        Instant firstSeen = Instant.parse("2026-09-18T07:00:00Z");
        Instant secondSeen = Instant.parse("2026-09-18T08:00:00Z");

        persistenceService.synchronize(
                List.of(repositorySummary(1001L, "one"), repositorySummary(1002L, "two")),
                firstSeen);

        persistenceService.synchronize(List.of(repositorySummary(1001L, "one")), secondSeen);

        assertTrue(repository.findByGitHubRepositoryId(1001L).orElseThrow().active);
        assertFalse(repository.findByGitHubRepositoryId(1002L).orElseThrow().active);
    }

    @Test
    @Transactional
    void emptySuccessfulDiscoveryMarksAllRepositoriesInactive() {
        persistenceService.synchronize(
                List.of(repositorySummary(1001L, "one")),
                Instant.parse("2026-09-18T07:00:00Z"));

        persistenceService.synchronize(List.of(), Instant.parse("2026-09-18T08:00:00Z"));

        assertFalse(repository.findByGitHubRepositoryId(1001L).orElseThrow().active);
    }

    private RepositorySummary repositorySummary(long id, String name) {
        return new RepositorySummary(
                id,
                "erland",
                name,
                "erland/" + name,
                "https://github.com/erland/" + name,
                RepositoryVisibility.PUBLIC,
                false,
                false,
                "main",
                List.of(),
                List.of(),
                null,
                new LicenseStatus(AnalysisState.NOT_ANALYZED, LicensePresence.UNKNOWN, null, null, null),
                new GitHubActionsStatus(AnalysisState.NOT_ANALYZED, null, null),
                new ReleaseStatus(AnalysisState.NOT_ANALYZED, null, null, null, null, null),
                new ActivityStatus(
                        Instant.parse("2026-09-17T12:00:00Z"),
                        Instant.parse("2026-09-17T12:05:00Z")),
                new RepositoryRefreshStatus(AnalysisState.NOT_ANALYZED, "pending"));
    }
}
