package info.isaksson.erland.repofleet.standards;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import info.isaksson.erland.repofleet.repository.api.ActivityStatus;
import info.isaksson.erland.repofleet.repository.api.AnalysisState;
import info.isaksson.erland.repofleet.repository.api.GitHubActionsStatus;
import info.isaksson.erland.repofleet.repository.api.LicensePresence;
import info.isaksson.erland.repofleet.repository.api.LicenseStatus;
import info.isaksson.erland.repofleet.repository.api.ReleaseStatus;
import info.isaksson.erland.repofleet.repository.api.RepositoryRefreshStatus;
import info.isaksson.erland.repofleet.repository.api.RepositorySummary;
import info.isaksson.erland.repofleet.repository.api.RepositoryVisibility;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RepositoryComplianceEvaluationServiceTest {

    @Test
    void evaluatesApplicableRulesInStableRuleKeyOrder() {
        Instant now = Instant.parse("2026-09-18T12:00:00Z");
        RepositorySummary repository = repository(now);

        RepositoryRuleAssignmentService assignments = mock(RepositoryRuleAssignmentService.class);
        RepositoryStandardRuleDefinition zRule = rule(
                "z-release",
                RepositoryRuleType.PUBLISHED_RELEASE_REQUIRED,
                now);
        RepositoryStandardRuleDefinition aRule = rule(
                "a-license",
                RepositoryRuleType.LICENSE_REQUIRED,
                now);

        when(assignments.applicableRules(repository)).thenReturn(List.of(
                new ApplicableRepositoryRule(
                        zRule,
                        ApplicableRepositoryRule.ApplicationReason.ALL_REPOSITORIES,
                        List.of()),
                new ApplicableRepositoryRule(
                        aRule,
                        ApplicableRepositoryRule.ApplicationReason.ALL_REPOSITORIES,
                        List.of())));

        RepositoryComplianceEvaluationService service =
                new RepositoryComplianceEvaluationService(
                        assignments,
                        new RepositoryRuleEvaluator(),
                        Clock.fixed(now, ZoneOffset.UTC));

        var evaluations = service.evaluate(repository);

        assertEquals(
                List.of("a-license", "z-release"),
                evaluations.stream().map(RepositoryRuleEvaluation::ruleKey).toList());
    }

    private RepositoryStandardRuleDefinition rule(
            String key,
            RepositoryRuleType type,
            Instant now) {
        return new RepositoryStandardRuleDefinition(
                key,
                type,
                key,
                null,
                RepositoryRuleSeverity.REQUIRED,
                true,
                Map.of(),
                RepositoryRuleScope.ALL_REPOSITORIES,
                now,
                now);
    }

    private RepositorySummary repository(Instant now) {
        return new RepositorySummary(
                1L,
                "erland",
                "repo",
                "erland/repo",
                "https://github.com/erland/repo",
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
                        "MIT"),
                new GitHubActionsStatus(AnalysisState.COMPLETE, true, 1),
                new ReleaseStatus(AnalysisState.COMPLETE, true, "v1", "v1", now, false),
                new ActivityStatus(now, now),
                new RepositoryRefreshStatus(AnalysisState.COMPLETE, "complete"));
    }
}
