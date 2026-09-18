package info.isaksson.erland.repofleet.acceptance;

import static org.junit.jupiter.api.Assertions.*;

import info.isaksson.erland.repofleet.repository.api.*;
import info.isaksson.erland.repofleet.repository.persistence.*;
import info.isaksson.erland.repofleet.repository.refresh.*;
import info.isaksson.erland.repofleet.standards.*;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class Phase2CoreAcceptanceTest {

    @Inject RepositoryIdentityRepository identities;
    @Inject RepositoryEnrichmentSnapshotRepository snapshots;
    @Inject RepositoryEnrichmentSnapshotService snapshotService;
    @Inject CachedRepositoryInventoryService cachedInventory;
    @Inject RepositoryRefreshPlanner planner;
    @Inject RepositoryGroupService groups;
    @Inject RepositoryStandardRuleService rules;
    @Inject RepositoryRuleAssignmentService assignments;
    @Inject RepositoryComplianceEvaluationService compliance;
    @Inject RepositoryComplianceExceptionService exceptions;
    @Inject RepositoryRefreshQueueService refreshQueue;

    @BeforeEach
    @Transactional
    void clearDatabase() {
        RepositoryRefreshJob.deleteAll();
        RepositoryComplianceException.deleteAll();
        RepositoryComplianceResult.deleteAll();
        RepositoryRuleGroupAssignment.deleteAll();
        RepositoryGroup.deleteAll();
        RepositoryStandardRule.deleteAll();
        snapshots.deleteAll();
        identities.deleteAll();
    }

    @Test
    @Transactional
    void phase2CoreBehaviorSurvivesPersistenceAndRemainsIncremental() {
        Instant now = Instant.now();
        RepositorySummary repository = completeRepository(8101L, "svc-acceptance", now);

        var identity = identities.insert(
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
        identity.changeClassification = RepositoryChangeClassification.APPARENTLY_UNCHANGED.name();
        snapshotService.saveSnapshot(repository, now, null);

        // Persistence/restart acceptance: reconstruct solely from persisted DB state.
        var reloaded = cachedInventory.loadActiveRepositories();
        assertEquals(1, reloaded.size());
        assertEquals(repository.fullName(), reloaded.getFirst().fullName());
        assertEquals("Java", reloaded.getFirst().primaryLanguage());
        assertEquals(LicensePresence.PRESENT, reloaded.getFirst().license().presence());

        // Incremental acceptance: a fresh unchanged repository must be reused, not enriched again.
        var plan = planner.plan(List.of(repository));
        assertEquals(1, plan.reusedCount());
        assertEquals(0, plan.scheduledCount());
        assertEquals(RepositoryRefreshAction.REUSE_CACHED, plan.items().getFirst().action());
        assertNotNull(plan.items().getFirst().cached());

        // Groups + selected rule assignment.
        groups.save(
                "services",
                "Services",
                null,
                new RepositoryGroupSelector(
                        "svc-",
                        null,
                        null,
                        null,
                        null,
                        List.of(),
                        List.of()),
                true,
                now);
        rules.save(
                "required-architecture-topic",
                RepositoryRuleType.REQUIRED_TOPIC,
                "Architecture topic",
                null,
                RepositoryRuleSeverity.REQUIRED,
                true,
                Map.of("topic", "architecture"),
                RepositoryRuleScope.SELECTED_GROUPS,
                now);
        assignments.assign("required-architecture-topic", "services", now);

        // UNKNOWN semantics are explicit and not silently converted to FAIL.
        rules.save(
                "readme-required",
                RepositoryRuleType.README_REQUIRED,
                "README required",
                null,
                RepositoryRuleSeverity.RECOMMENDED,
                true,
                Map.of(),
                RepositoryRuleScope.ALL_REPOSITORIES,
                now);

        var evaluations = compliance.evaluate(repository);
        assertEquals(2, evaluations.size());

        var topic = evaluations.stream()
                .filter(result -> result.ruleKey().equals("required-architecture-topic"))
                .findFirst()
                .orElseThrow();
        assertEquals(RepositoryRuleEvaluationResult.PASS, topic.result());

        var readme = evaluations.stream()
                .filter(result -> result.ruleKey().equals("readme-required"))
                .findFirst()
                .orElseThrow();
        assertEquals(RepositoryRuleEvaluationResult.UNKNOWN, readme.result());

        // Exceptions remain persisted RepoFleet state.
        var accepted = exceptions.save(
                repository.id(),
                "readme-required",
                "Accepted until README signal is reliable.",
                now.plusSeconds(3600),
                now);
        assertEquals(RepositoryComplianceExceptionState.ACTIVE, accepted.state());
        assertTrue(exceptions.hasActiveException(repository.id(), "readme-required"));

        // Targeted refresh queue is persistent and deduplicated.
        var firstJob = refreshQueue.enqueue(repository.id(), "MANUAL_SINGLE_REPOSITORY", now);
        var duplicateJob = refreshQueue.enqueue(repository.id(), "WEBHOOK_PUSH", now.plusSeconds(1));
        assertEquals(firstJob.id(), duplicateJob.id());
        assertEquals(1L, RepositoryRefreshJob.count());
    }

    private RepositorySummary completeRepository(long id, String name, Instant now) {
        return new RepositorySummary(
                id,
                "erland",
                name,
                "erland/" + name,
                "https://github.com/erland/" + name,
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
                new RepositoryRefreshStatus(
                        AnalysisState.COMPLETE,
                        "Persisted acceptance fixture.",
                        CacheFreshness.FRESH));
    }
}
