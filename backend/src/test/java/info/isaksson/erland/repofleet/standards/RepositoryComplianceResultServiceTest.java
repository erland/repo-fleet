package info.isaksson.erland.repofleet.standards;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
import info.isaksson.erland.repofleet.repository.persistence.RepositoryEnrichmentSnapshot;
import info.isaksson.erland.repofleet.repository.persistence.RepositoryEnrichmentSnapshotRepository;
import info.isaksson.erland.repofleet.repository.persistence.RepositoryEnrichmentSnapshotService;
import info.isaksson.erland.repofleet.repository.persistence.RepositoryIdentityRepository;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class RepositoryComplianceResultServiceTest {

    @Inject
    RepositoryComplianceResultService compliance;

    @Inject
    RepositoryStandardRuleService rules;

    @Inject
    RepositoryIdentityRepository identities;

    @Inject
    RepositoryEnrichmentSnapshotRepository snapshots;

    @Inject
    RepositoryEnrichmentSnapshotService snapshotService;

    @BeforeEach
    @Transactional
    void clearDatabase() {
        RepositoryComplianceResult.deleteAll();
        RepositoryRuleGroupAssignment.deleteAll();
        RepositoryGroup.deleteAll();
        RepositoryStandardRule.deleteAll();
        snapshots.deleteAll();
        identities.deleteAll();
    }

    @Test
    @Transactional
    void persistsReusesAndReevaluatesWhenSourceChanges() {
        Instant sourceTime = Instant.parse("2026-09-18T10:00:00Z");
        RepositorySummary repository = repository();

        identities.insert(
                repository.id(),
                repository.owner(),
                repository.name(),
                repository.fullName(),
                repository.visibility(),
                repository.archived(),
                repository.fork(),
                repository.defaultBranch(),
                sourceTime,
                sourceTime,
                sourceTime);

        snapshotService.saveSnapshot(repository, sourceTime, null);

        rules.save(
                "license-required",
                RepositoryRuleType.LICENSE_REQUIRED,
                "License required",
                null,
                RepositoryRuleSeverity.REQUIRED,
                true,
                Map.of(),
                RepositoryRuleScope.ALL_REPOSITORIES,
                sourceTime);

        var first = compliance.evaluateAndPersist(repository);
        assertEquals(1, first.size());
        assertEquals(RepositoryRuleEvaluationResult.PASS, first.getFirst().evaluation().result());

        RepositoryComplianceResult persisted = RepositoryComplianceResult.find(
                        "githubRepositoryId = ?1 and ruleKey = ?2",
                        repository.id(),
                        "license-required")
                .firstResult();
        persisted.reason = "persisted marker";

        var reused = compliance.evaluateAndPersist(repository);
        assertEquals("persisted marker", reused.getFirst().evaluation().reason());

        RepositoryEnrichmentSnapshot snapshot = snapshots
                .findByGitHubRepositoryId(repository.id())
                .orElseThrow();
        snapshot.updatedAt = sourceTime.plusSeconds(60);

        var reevaluated = compliance.evaluateAndPersist(repository);
        assertNotEquals("persisted marker", reevaluated.getFirst().evaluation().reason());
        assertEquals(
                sourceTime.plusSeconds(60),
                reevaluated.getFirst().sourceUpdatedAt());

        var stored = compliance.listForRepository(repository.id());
        assertEquals(1, stored.size());
        assertEquals("license-required", stored.getFirst().evaluation().ruleKey());
    }

    @Test
    @Transactional
    void removesPersistedResultWhenRuleNoLongerApplies() {
        Instant now = Instant.parse("2026-09-18T10:00:00Z");
        RepositorySummary repository = repository();

        identities.insert(
                repository.id(),
                repository.owner(),
                repository.name(),
                repository.fullName(),
                repository.visibility(),
                repository.archived(),
                repository.fork(),
                repository.defaultBranch(),
                now,
                now,
                now);

        snapshotService.saveSnapshot(repository, now, null);

        rules.save(
                "license-required",
                RepositoryRuleType.LICENSE_REQUIRED,
                "License required",
                null,
                RepositoryRuleSeverity.REQUIRED,
                true,
                Map.of(),
                RepositoryRuleScope.ALL_REPOSITORIES,
                now);

        assertEquals(1, compliance.evaluateAndPersist(repository).size());

        rules.save(
                "license-required",
                RepositoryRuleType.LICENSE_REQUIRED,
                "License required",
                null,
                RepositoryRuleSeverity.REQUIRED,
                false,
                Map.of(),
                RepositoryRuleScope.ALL_REPOSITORIES,
                now.plusSeconds(60));

        assertTrue(compliance.evaluateAndPersist(repository).isEmpty());
        assertTrue(compliance.listForRepository(repository.id()).isEmpty());
    }

    private RepositorySummary repository() {
        Instant now = Instant.parse("2026-09-18T10:00:00Z");
        return new RepositorySummary(
                1001L,
                "erland",
                "repo-fleet",
                "erland/repo-fleet",
                "https://github.com/erland/repo-fleet",
                RepositoryVisibility.PRIVATE,
                false,
                false,
                "main",
                List.of("architecture"),
                List.of("Java"),
                "Java",
                new LicenseStatus(
                        AnalysisState.COMPLETE,
                        LicensePresence.PRESENT,
                        true,
                        "mit",
                        "MIT License"),
                new GitHubActionsStatus(AnalysisState.COMPLETE, true, 2),
                new ReleaseStatus(
                        AnalysisState.COMPLETE,
                        true,
                        "v1.0.0",
                        "v1.0.0",
                        now,
                        false),
                new ActivityStatus(now, now),
                new RepositoryRefreshStatus(AnalysisState.COMPLETE, "complete"));
    }
}
