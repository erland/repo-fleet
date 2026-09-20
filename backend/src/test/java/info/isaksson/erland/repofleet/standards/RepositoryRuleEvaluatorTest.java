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

class RepositoryRuleEvaluatorTest {

    private static final Instant NOW = Instant.parse("2026-09-18T10:00:00Z");
    private final RepositoryRuleEvaluator evaluator = new RepositoryRuleEvaluator();

    @Test
    void evaluatesLicensePassFailAndUnknown() {
        assertEquals(
                RepositoryRuleEvaluationResult.PASS,
                evaluator.evaluate(repository(), rule("license", RepositoryRuleType.LICENSE_REQUIRED, Map.of()), NOW).result());

        RepositorySummary missing = withLicense(
                new LicenseStatus(AnalysisState.COMPLETE, LicensePresence.MISSING, false, null, null));
        assertEquals(
                RepositoryRuleEvaluationResult.FAIL,
                evaluator.evaluate(missing, rule("license", RepositoryRuleType.LICENSE_REQUIRED, Map.of()), NOW).result());

        RepositorySummary unknown = withLicense(
                new LicenseStatus(AnalysisState.PARTIAL, LicensePresence.UNKNOWN, null, null, null));
        assertEquals(
                RepositoryRuleEvaluationResult.UNKNOWN,
                evaluator.evaluate(unknown, rule("license", RepositoryRuleType.LICENSE_REQUIRED, Map.of()), NOW).result());
    }

    @Test
    void evaluatesActionsAndRelease() {
        assertEquals(
                RepositoryRuleEvaluationResult.PASS,
                evaluator.evaluate(repository(), rule("actions", RepositoryRuleType.ACTIONS_WORKFLOW_REQUIRED, Map.of()), NOW).result());
        assertEquals(
                RepositoryRuleEvaluationResult.PASS,
                evaluator.evaluate(repository(), rule("release", RepositoryRuleType.PUBLISHED_RELEASE_REQUIRED, Map.of()), NOW).result());

        RepositorySummary noActions = withActions(new GitHubActionsStatus(AnalysisState.COMPLETE, false, 0));
        assertEquals(
                RepositoryRuleEvaluationResult.FAIL,
                evaluator.evaluate(noActions, rule("actions", RepositoryRuleType.ACTIONS_WORKFLOW_REQUIRED, Map.of()), NOW).result());

        RepositorySummary unknownRelease = withRelease(new ReleaseStatus(
                AnalysisState.FAILED, null, null, null, null, null));
        assertEquals(
                RepositoryRuleEvaluationResult.UNKNOWN,
                evaluator.evaluate(unknownRelease, rule("release", RepositoryRuleType.PUBLISHED_RELEASE_REQUIRED, Map.of()), NOW).result());
    }

    @Test
    void evaluatesRequiredTopicCaseInsensitively() {
        assertEquals(
                RepositoryRuleEvaluationResult.PASS,
                evaluator.evaluate(
                        repository(),
                        rule("topic", RepositoryRuleType.REQUIRED_TOPIC, Map.of("topic", "ARCHITECTURE")),
                        NOW).result());

        assertEquals(
                RepositoryRuleEvaluationResult.FAIL,
                evaluator.evaluate(
                        repository(),
                        rule("topic", RepositoryRuleType.REQUIRED_TOPIC, Map.of("topic", "security")),
                        NOW).result());

        RepositorySummary partialTopics = withTopics(
                List.of(),
                new RepositoryRefreshStatus(AnalysisState.PARTIAL, "partial"));
        assertEquals(
                RepositoryRuleEvaluationResult.UNKNOWN,
                evaluator.evaluate(
                        partialTopics,
                        rule("topic", RepositoryRuleType.REQUIRED_TOPIC, Map.of("topic", "architecture")),
                        NOW).result());

        assertEquals(
                RepositoryRuleEvaluationResult.NOT_APPLICABLE,
                evaluator.evaluate(
                        repository(),
                        rule("topic", RepositoryRuleType.REQUIRED_TOPIC, Map.of()),
                        NOW).result());
    }

    @Test
    void evaluatesMaximumInactivityAndUnknownActivity() {
        assertEquals(
                RepositoryRuleEvaluationResult.PASS,
                evaluator.evaluate(
                        repository(),
                        rule("activity", RepositoryRuleType.MAXIMUM_INACTIVITY_AGE, Map.of("days", 30)),
                        NOW).result());

        assertEquals(
                RepositoryRuleEvaluationResult.FAIL,
                evaluator.evaluate(
                        repository(),
                        rule("activity", RepositoryRuleType.MAXIMUM_INACTIVITY_AGE, Map.of("days", 3)),
                        NOW).result());

        RepositorySummary unknownActivity = withActivity(new ActivityStatus(null, null));
        assertEquals(
                RepositoryRuleEvaluationResult.UNKNOWN,
                evaluator.evaluate(
                        unknownActivity,
                        rule("activity", RepositoryRuleType.MAXIMUM_INACTIVITY_AGE, Map.of("days", 30)),
                        NOW).result());

        assertEquals(
                RepositoryRuleEvaluationResult.NOT_APPLICABLE,
                evaluator.evaluate(
                        repository(),
                        rule("activity", RepositoryRuleType.MAXIMUM_INACTIVITY_AGE, Map.of("days", -1)),
                        NOW).result());
    }

    @Test
    void usesUpdatedAtWhenPushedAtIsMissingForInactivityEvaluation() {
        RepositorySummary recentUpdate = withActivity(
                new ActivityStatus(null, NOW.minusSeconds(20 * 24 * 3600)));
        assertEquals(
                RepositoryRuleEvaluationResult.PASS,
                evaluator.evaluate(
                        recentUpdate,
                        rule("activity", RepositoryRuleType.MAXIMUM_INACTIVITY_AGE, Map.of("days", 30)),
                        NOW).result());

        RepositorySummary oldUpdate = withActivity(
                new ActivityStatus(null, NOW.minusSeconds(60 * 24 * 3600)));
        RepositoryRuleEvaluation evaluation = evaluator.evaluate(
                oldUpdate,
                rule("activity", RepositoryRuleType.MAXIMUM_INACTIVITY_AGE, Map.of("days", 30)),
                NOW);
        assertEquals(RepositoryRuleEvaluationResult.FAIL, evaluation.result());
        assertEquals("60 days", evaluation.observedValue());
    }

    @Test
    void readmeRuleIsUnknownUntilReliableSignalExists() {
        assertEquals(
                RepositoryRuleEvaluationResult.UNKNOWN,
                evaluator.evaluate(
                        repository(),
                        rule("readme", RepositoryRuleType.README_REQUIRED, Map.of()),
                        NOW).result());
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
                new LicenseStatus(AnalysisState.COMPLETE, LicensePresence.PRESENT, true, "mit", "MIT License"),
                new GitHubActionsStatus(AnalysisState.COMPLETE, true, 2),
                new ReleaseStatus(AnalysisState.COMPLETE, true, "v1.0", "v1.0", NOW.minusSeconds(3600), false),
                new ActivityStatus(NOW.minusSeconds(10 * 24 * 3600), NOW.minusSeconds(5 * 24 * 3600)),
                new RepositoryRefreshStatus(AnalysisState.COMPLETE, "complete"));
    }

    private RepositorySummary withLicense(LicenseStatus license) {
        RepositorySummary r = repository();
        return copy(r, license, r.githubActions(), r.release(), r.activity());
    }

    private RepositorySummary withActions(GitHubActionsStatus actions) {
        RepositorySummary r = repository();
        return copy(r, r.license(), actions, r.release(), r.activity());
    }

    private RepositorySummary withRelease(ReleaseStatus release) {
        RepositorySummary r = repository();
        return copy(r, r.license(), r.githubActions(), release, r.activity());
    }

    private RepositorySummary withActivity(ActivityStatus activity) {
        RepositorySummary r = repository();
        return copy(r, r.license(), r.githubActions(), r.release(), activity);
    }

    private RepositorySummary withTopics(
            List<String> topics,
            RepositoryRefreshStatus refreshStatus) {
        RepositorySummary r = repository();
        return new RepositorySummary(
                r.id(), r.owner(), r.name(), r.fullName(), r.url(), r.visibility(),
                r.archived(), r.fork(), r.defaultBranch(), topics, r.languages(),
                r.primaryLanguage(), r.license(), r.githubActions(), r.release(),
                r.activity(), refreshStatus);
    }

    private RepositorySummary copy(
            RepositorySummary r,
            LicenseStatus license,
            GitHubActionsStatus actions,
            ReleaseStatus release,
            ActivityStatus activity) {
        return new RepositorySummary(
                r.id(), r.owner(), r.name(), r.fullName(), r.url(), r.visibility(),
                r.archived(), r.fork(), r.defaultBranch(), r.topics(), r.languages(),
                r.primaryLanguage(), license, actions, release, activity, r.refreshStatus());
    }
}
