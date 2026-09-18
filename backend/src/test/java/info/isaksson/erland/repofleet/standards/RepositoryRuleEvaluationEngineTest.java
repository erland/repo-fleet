package info.isaksson.erland.repofleet.standards;

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
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RepositoryRuleEvaluationEngineTest {

    private static final Instant NOW = Instant.parse("2026-09-18T12:00:00Z");

    private final RepositoryRuleEvaluationEngine engine =
            new RepositoryRuleEvaluationEngine();

    @Test
    void evaluatesLicensePassAndFail() {
        var pass = engine.evaluate(
                repository(
                        new LicenseStatus(AnalysisState.COMPLETE, LicensePresence.PRESENT, true, "mit", "MIT"),
                        completeActions(true),
                        completeRelease(true),
                        List.of("architecture"),
                        new ActivityStatus(NOW.minusSeconds(10 * 86400), null)),
                rule("license", RepositoryRuleType.LICENSE_REQUIRED, Map.of()),
                NOW);

        var fail = engine.evaluate(
                repository(
                        new LicenseStatus(AnalysisState.COMPLETE, LicensePresence.MISSING, false, null, null),
                        completeActions(true),
                        completeRelease(true),
                        List.of("architecture"),
                        new ActivityStatus(NOW.minusSeconds(10 * 86400), null)),
                rule("license", RepositoryRuleType.LICENSE_REQUIRED, Map.of()),
                NOW);

        assertEquals(RepositoryRuleEvaluationResult.PASS, pass.result());
        assertEquals(RepositoryRuleEvaluationResult.FAIL, fail.result());
    }

    @Test
    void unknownInventoryDataNeverBecomesFailure() {
        RepositorySummary repository = repository(
                new LicenseStatus(AnalysisState.NOT_ANALYZED, LicensePresence.UNKNOWN, null, null, null),
                new GitHubActionsStatus(AnalysisState.FAILED, null, null),
                new ReleaseStatus(AnalysisState.NOT_ANALYZED, null, null, null, null, null),
                List.of(),
                new ActivityStatus(null, null));

        assertEquals(
                RepositoryRuleEvaluationResult.UNKNOWN,
                engine.evaluate(repository, rule("license", RepositoryRuleType.LICENSE_REQUIRED, Map.of()), NOW).result());
        assertEquals(
                RepositoryRuleEvaluationResult.UNKNOWN,
                engine.evaluate(repository, rule("actions", RepositoryRuleType.ACTIONS_WORKFLOW_REQUIRED, Map.of()), NOW).result());
        assertEquals(
                RepositoryRuleEvaluationResult.UNKNOWN,
                engine.evaluate(repository, rule("release", RepositoryRuleType.PUBLISHED_RELEASE_REQUIRED, Map.of()), NOW).result());
        assertEquals(
                RepositoryRuleEvaluationResult.UNKNOWN,
                engine.evaluate(repository, rule("activity", RepositoryRuleType.MAXIMUM_INACTIVITY_AGE, Map.of("days", 30)), NOW).result());
    }

    @Test
    void evaluatesRequiredTopicCaseInsensitively() {
        var evaluation = engine.evaluate(
                repository(
                        completeLicense(),
                        completeActions(true),
                        completeRelease(true),
                        List.of("Architecture", "backend"),
                        new ActivityStatus(NOW.minusSeconds(10 * 86400), null)),
                rule("topic", RepositoryRuleType.REQUIRED_TOPIC, Map.of("topic", "architecture")),
                NOW);

        assertEquals(RepositoryRuleEvaluationResult.PASS, evaluation.result());
        assertEquals("Architecture,backend", evaluation.observedValue());
    }

    @Test
    void missingRuleParameterIsNotApplicable() {
        var evaluation = engine.evaluate(
                repository(
                        completeLicense(),
                        completeActions(true),
                        completeRelease(true),
                        List.of(),
                        new ActivityStatus(NOW.minusSeconds(10 * 86400), null)),
                rule("topic", RepositoryRuleType.REQUIRED_TOPIC, Map.of()),
                NOW);

        assertEquals(RepositoryRuleEvaluationResult.NOT_APPLICABLE, evaluation.result());
    }

    @Test
    void evaluatesMaximumInactivityUsingUpdatedAtWhenPushedAtIsMissing() {
        var pass = engine.evaluate(
                repository(
                        completeLicense(),
                        completeActions(true),
                        completeRelease(true),
                        List.of(),
                        new ActivityStatus(null, NOW.minusSeconds(20 * 86400))),
                rule("activity", RepositoryRuleType.MAXIMUM_INACTIVITY_AGE, Map.of("days", 30)),
                NOW);

        var fail = engine.evaluate(
                repository(
                        completeLicense(),
                        completeActions(true),
                        completeRelease(true),
                        List.of(),
                        new ActivityStatus(null, NOW.minusSeconds(60 * 86400))),
                rule("activity", RepositoryRuleType.MAXIMUM_INACTIVITY_AGE, Map.of("days", 30)),
                NOW);

        assertEquals(RepositoryRuleEvaluationResult.PASS, pass.result());
        assertEquals(RepositoryRuleEvaluationResult.FAIL, fail.result());
        assertEquals("60 days", fail.observedValue());
    }

    @Test
    void readmeRuleIsUnknownUntilInventorySupportsReadmePresence() {
        var evaluation = engine.evaluate(
                repository(
                        completeLicense(),
                        completeActions(true),
                        completeRelease(true),
                        List.of(),
                        new ActivityStatus(NOW, null)),
                rule("readme", RepositoryRuleType.README_REQUIRED, Map.of()),
                NOW);

        assertEquals(RepositoryRuleEvaluationResult.UNKNOWN, evaluation.result());
    }

    private RepositoryStandardRuleDefinition rule(
            String key,
            RepositoryRuleType type,
            Map<String, Object> parameters) {
        return new RepositoryStandardRuleDefinition(
                key,
                type,
                key,
                null,
                RepositoryRuleSeverity.REQUIRED,
                true,
                parameters,
                RepositoryRuleScope.ALL_REPOSITORIES,
                NOW,
                NOW);
    }

    private RepositorySummary repository(
            LicenseStatus license,
            GitHubActionsStatus actions,
            ReleaseStatus release,
            List<String> topics,
            ActivityStatus activity) {
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
                topics,
                List.of("Java"),
                "Java",
                license,
                actions,
                release,
                activity,
                new RepositoryRefreshStatus(AnalysisState.COMPLETE, "complete"));
    }

    private LicenseStatus completeLicense() {
        return new LicenseStatus(AnalysisState.COMPLETE, LicensePresence.PRESENT, true, "mit", "MIT");
    }

    private GitHubActionsStatus completeActions(boolean present) {
        return new GitHubActionsStatus(AnalysisState.COMPLETE, present, present ? 1 : 0);
    }

    private ReleaseStatus completeRelease(boolean present) {
        return new ReleaseStatus(
                AnalysisState.COMPLETE,
                present,
                present ? "v1" : null,
                present ? "v1" : null,
                present ? NOW.minusSeconds(86400) : null,
                false);
    }
}
