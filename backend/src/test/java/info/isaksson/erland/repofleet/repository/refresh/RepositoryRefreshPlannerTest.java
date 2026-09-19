package info.isaksson.erland.repofleet.repository.refresh;

import static org.junit.jupiter.api.Assertions.assertEquals;

import info.isaksson.erland.repofleet.repository.api.ActivityStatus;
import info.isaksson.erland.repofleet.repository.api.AnalysisState;
import info.isaksson.erland.repofleet.repository.api.GitHubActionsStatus;
import info.isaksson.erland.repofleet.repository.api.LicensePresence;
import info.isaksson.erland.repofleet.repository.api.LicenseStatus;
import info.isaksson.erland.repofleet.repository.api.ReleaseStatus;
import info.isaksson.erland.repofleet.repository.api.RepositoryRefreshStatus;
import info.isaksson.erland.repofleet.repository.api.RepositorySummary;
import info.isaksson.erland.repofleet.repository.api.RepositoryVisibility;
import info.isaksson.erland.repofleet.repository.persistence.RepositoryEnrichmentSnapshot;
import info.isaksson.erland.repofleet.repository.persistence.RepositoryEnrichmentSnapshotRepository;
import info.isaksson.erland.repofleet.repository.persistence.RepositoryEnrichmentSnapshotService;
import info.isaksson.erland.repofleet.repository.persistence.RepositoryIdentityRepository;
import info.isaksson.erland.repofleet.repository.persistence.RepositoryInventoryPersistenceService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class RepositoryRefreshPlannerTest {

    @Inject
    RepositoryRefreshPlanner planner;

    @Inject
    RepositoryInventoryPersistenceService inventoryPersistence;

    @Inject
    RepositoryIdentityRepository identityRepository;

    @Inject
    RepositoryEnrichmentSnapshotRepository snapshotRepository;

    @Inject
    RepositoryEnrichmentSnapshotService snapshotService;

    @BeforeEach
    @Transactional
    void clearDatabase() {
        snapshotRepository.deleteAll();
        identityRepository.deleteAll();
    }

    @Test
    @Transactional
    void plansNewRepositoryForFullNewEnrichment() {
        RepositorySummary discovered = repository(1001L, RepositoryVisibility.PUBLIC, "main");

        inventoryPersistence.synchronize(
                List.of(discovered),
                Instant.now());

        RepositoryRefreshPlan plan = planner.plan(List.of(discovered));

        assertEquals(RepositoryRefreshAction.FULL_ENRICHMENT_NEW, plan.items().getFirst().action());
        assertEquals(0, plan.reusedCount());
        assertEquals(1, plan.newCount());
        assertEquals(0, plan.changedCount());
        assertEquals(1, plan.scheduledCount());
    }

    @Test
    @Transactional
    void reusesCompleteSnapshotForApparentlyUnchangedRepository() {
        RepositorySummary discovered = repository(1001L, RepositoryVisibility.PUBLIC, "main");
        inventoryPersistence.synchronize(
                List.of(discovered),
                Instant.now().minusSeconds(120));
        snapshotService.saveSnapshot(
                complete(discovered),
                Instant.now().minusSeconds(60),
                null);

        inventoryPersistence.synchronize(
                List.of(discovered),
                Instant.now());

        RepositoryRefreshPlan plan = planner.plan(List.of(discovered));

        assertEquals(RepositoryRefreshAction.REUSE_CACHED, plan.items().getFirst().action());
        assertEquals(1, plan.reusedCount());
        assertEquals(0, plan.newCount());
        assertEquals(0, plan.changedCount());
        assertEquals(0, plan.scheduledCount());
        assertEquals(
                AnalysisState.COMPLETE,
                plan.items().getFirst().cached().refreshStatus().state());
    }

    @Test
    @Transactional
    void schedulesApparentlyUnchangedRepositoryWhenSnapshotIsStale() {
        RepositorySummary discovered = repository(1001L, RepositoryVisibility.PUBLIC, "main");
        inventoryPersistence.synchronize(
                List.of(discovered),
                Instant.now().minusSeconds(7200));
        snapshotService.saveSnapshot(
                complete(discovered),
                Instant.now().minusSeconds(7200),
                null);

        inventoryPersistence.synchronize(
                List.of(discovered),
                Instant.now());

        RepositoryRefreshPlan plan = planner.plan(List.of(discovered));

        assertEquals(RepositoryRefreshAction.FULL_ENRICHMENT, plan.items().getFirst().action());
        assertEquals(AnalysisState.COMPLETE, plan.items().getFirst().cached().refreshStatus().state());
        assertEquals(LicensePresence.PRESENT, plan.items().getFirst().cached().license().presence());
        assertEquals(0, plan.reusedCount());
        assertEquals(1, plan.scheduledCount());
    }

    @Test
    @Transactional
    void schedulesChangedRepositoryForFullEnrichment() {
        RepositorySummary original = repository(1001L, RepositoryVisibility.PUBLIC, "main");
        inventoryPersistence.synchronize(
                List.of(original),
                Instant.parse("2026-09-18T07:00:00Z"));
        snapshotService.saveSnapshot(
                complete(original),
                Instant.parse("2026-09-18T07:05:00Z"),
                null);

        RepositorySummary changed = repository(1001L, RepositoryVisibility.PRIVATE, "trunk");
        inventoryPersistence.synchronize(
                List.of(changed),
                Instant.parse("2026-09-18T08:00:00Z"));

        RepositoryRefreshPlan plan = planner.plan(List.of(changed));

        assertEquals(RepositoryRefreshAction.FULL_ENRICHMENT, plan.items().getFirst().action());
        assertEquals(AnalysisState.COMPLETE, plan.items().getFirst().cached().refreshStatus().state());
        assertEquals(LicensePresence.PRESENT, plan.items().getFirst().cached().license().presence());
        assertEquals(0, plan.reusedCount());
        assertEquals(0, plan.newCount());
        assertEquals(1, plan.changedCount());
        assertEquals(1, plan.scheduledCount());
    }

    @Test
    @Transactional
    void schedulesUnchangedRepositoryWhenSnapshotIsNotComplete() {
        RepositorySummary discovered = repository(1001L, RepositoryVisibility.PUBLIC, "main");
        inventoryPersistence.synchronize(
                List.of(discovered),
                Instant.parse("2026-09-18T07:00:00Z"));

        RepositoryEnrichmentSnapshot snapshot = new RepositoryEnrichmentSnapshot();
        snapshot.githubRepositoryId = 1001L;
        snapshot.topicsJson = "[]";
        snapshot.languagesJson = "[]";
        snapshot.licenseAnalysisState = AnalysisState.NOT_ANALYZED.name();
        snapshot.licensePresence = LicensePresence.UNKNOWN.name();
        snapshot.actionsAnalysisState = AnalysisState.NOT_ANALYZED.name();
        snapshot.releaseAnalysisState = AnalysisState.NOT_ANALYZED.name();
        snapshot.enrichmentState = AnalysisState.PARTIAL.name();
        snapshot.updatedAt = Instant.parse("2026-09-18T07:05:00Z");
        snapshotRepository.persist(snapshot);

        inventoryPersistence.synchronize(
                List.of(discovered),
                Instant.parse("2026-09-18T08:00:00Z"));

        RepositoryRefreshPlan plan = planner.plan(List.of(discovered));

        assertEquals(RepositoryRefreshAction.FULL_ENRICHMENT, plan.items().getFirst().action());
        assertEquals(0, plan.reusedCount());
        assertEquals(1, plan.scheduledCount());
    }

    private RepositorySummary repository(
            long id,
            RepositoryVisibility visibility,
            String defaultBranch) {
        return new RepositorySummary(
                id,
                "erland",
                "repo-" + id,
                "erland/repo-" + id,
                "https://github.com/erland/repo-" + id,
                visibility,
                false,
                false,
                defaultBranch,
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

    private RepositorySummary complete(RepositorySummary summary) {
        return new RepositorySummary(
                summary.id(),
                summary.owner(),
                summary.name(),
                summary.fullName(),
                summary.url(),
                summary.visibility(),
                summary.archived(),
                summary.fork(),
                summary.defaultBranch(),
                List.of("architecture"),
                List.of("Java"),
                "Java",
                new LicenseStatus(AnalysisState.COMPLETE, LicensePresence.PRESENT, true, "mit", "MIT License"),
                new GitHubActionsStatus(AnalysisState.COMPLETE, true, 2),
                new ReleaseStatus(
                        AnalysisState.COMPLETE,
                        true,
                        "v1.0.0",
                        "v1.0.0",
                        Instant.parse("2026-09-17T12:00:00Z"),
                        false),
                summary.activity(),
                new RepositoryRefreshStatus(AnalysisState.COMPLETE, "complete"));
    }
}
