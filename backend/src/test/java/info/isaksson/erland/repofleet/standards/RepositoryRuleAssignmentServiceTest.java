package info.isaksson.erland.repofleet.standards;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class RepositoryRuleAssignmentServiceTest {

    @Inject
    RepositoryStandardRuleService rules;

    @Inject
    RepositoryGroupService groups;

    @Inject
    RepositoryRuleAssignmentService assignments;

    @BeforeEach
    @Transactional
    void clearDatabase() {
        RepositoryRuleGroupAssignment.deleteAll();
        RepositoryGroup.deleteAll();
        RepositoryStandardRule.deleteAll();
    }

    @Test
    void globalRuleAppliesWithoutGroupMembership() {
        rules.save(
                "license-required",
                RepositoryRuleType.LICENSE_REQUIRED,
                "License required",
                null,
                RepositoryRuleSeverity.REQUIRED,
                true,
                Map.of(),
                RepositoryRuleScope.ALL_REPOSITORIES,
                Instant.parse("2026-09-18T09:00:00Z"));

        var applicable = assignments.applicableRules(repository());

        assertEquals(1, applicable.size());
        assertEquals(
                ApplicableRepositoryRule.ApplicationReason.ALL_REPOSITORIES,
                applicable.getFirst().reason());
        assertTrue(applicable.getFirst().matchingGroups().isEmpty());
    }

    @Test
    void selectedGroupRuleAppliesOnlyWhenRepositoryMatchesAssignedGroup() {
        rules.save(
                "actions-required",
                RepositoryRuleType.ACTIONS_WORKFLOW_REQUIRED,
                "Actions required",
                null,
                RepositoryRuleSeverity.REQUIRED,
                true,
                Map.of(),
                RepositoryRuleScope.SELECTED_GROUPS,
                Instant.parse("2026-09-18T09:00:00Z"));

        groups.save(
                "services",
                "Services",
                null,
                new RepositoryGroupSelector(
                        "svc-",
                        "erland",
                        RepositoryVisibility.PRIVATE,
                        false,
                        false,
                        List.of(),
                        List.of("Java")),
                true,
                Instant.parse("2026-09-18T09:00:00Z"));

        assignments.assign(
                "actions-required",
                "services",
                Instant.parse("2026-09-18T09:05:00Z"));

        var applicable = assignments.applicableRules(repository());

        assertEquals(1, applicable.size());
        assertEquals("actions-required", applicable.getFirst().rule().ruleKey());
        assertEquals(
                ApplicableRepositoryRule.ApplicationReason.MATCHING_GROUP,
                applicable.getFirst().reason());
        assertEquals(List.of("services"), applicable.getFirst().matchingGroups());
    }

    @Test
    void duplicateAssignmentsAcrossMatchingGroupsProduceSingleApplicableRule() {
        rules.save(
                "topic-required",
                RepositoryRuleType.REQUIRED_TOPIC,
                "Architecture topic required",
                null,
                RepositoryRuleSeverity.RECOMMENDED,
                true,
                Map.of("topic", "architecture"),
                RepositoryRuleScope.SELECTED_GROUPS,
                Instant.parse("2026-09-18T09:00:00Z"));

        groups.save(
                "architecture",
                "Architecture",
                null,
                new RepositoryGroupSelector(
                        null,
                        null,
                        null,
                        null,
                        null,
                        List.of("architecture"),
                        List.of()),
                true,
                Instant.parse("2026-09-18T09:00:00Z"));

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
                Instant.parse("2026-09-18T09:00:00Z"));

        assignments.assign("topic-required", "services", Instant.parse("2026-09-18T09:05:00Z"));
        assignments.assign("topic-required", "architecture", Instant.parse("2026-09-18T09:06:00Z"));
        assignments.assign("topic-required", "services", Instant.parse("2026-09-18T09:07:00Z"));

        var applicable = assignments.applicableRules(repository());

        assertEquals(1, applicable.size());
        assertEquals(
                List.of("architecture", "services"),
                applicable.getFirst().matchingGroups());
    }

    private RepositorySummary repository() {
        return new RepositorySummary(
                1L,
                "erland",
                "svc-orders",
                "erland/svc-orders",
                "https://github.com/erland/svc-orders",
                RepositoryVisibility.PRIVATE,
                false,
                false,
                "main",
                List.of("architecture"),
                List.of("Java"),
                "Java",
                new LicenseStatus(AnalysisState.COMPLETE, LicensePresence.PRESENT, true, "mit", "MIT"),
                new GitHubActionsStatus(AnalysisState.COMPLETE, true, 1),
                new ReleaseStatus(AnalysisState.COMPLETE, true, "v1", "v1", null, false),
                new ActivityStatus(null, null),
                new RepositoryRefreshStatus(AnalysisState.COMPLETE, "complete"));
    }
}
