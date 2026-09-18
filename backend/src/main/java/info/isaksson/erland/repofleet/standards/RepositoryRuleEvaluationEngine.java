package info.isaksson.erland.repofleet.standards;

import info.isaksson.erland.repofleet.repository.api.AnalysisState;
import info.isaksson.erland.repofleet.repository.api.LicensePresence;
import info.isaksson.erland.repofleet.repository.api.RepositorySummary;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@ApplicationScoped
public class RepositoryRuleEvaluationEngine {

    public RepositoryRuleEvaluation evaluate(
            RepositorySummary repository,
            RepositoryStandardRuleDefinition rule,
            Instant now) {
        return switch (rule.ruleType()) {
            case LICENSE_REQUIRED -> evaluateLicense(repository, rule);
            case ACTIONS_WORKFLOW_REQUIRED -> evaluateActions(repository, rule);
            case PUBLISHED_RELEASE_REQUIRED -> evaluateRelease(repository, rule);
            case REQUIRED_TOPIC -> evaluateRequiredTopic(repository, rule);
            case MAXIMUM_INACTIVITY_AGE -> evaluateMaximumInactivity(repository, rule, now);
            case README_REQUIRED -> result(
                    rule,
                    RepositoryRuleEvaluationResult.UNKNOWN,
                    "README presence is not available in the current inventory model.",
                    null);
        };
    }

    private RepositoryRuleEvaluation evaluateLicense(
            RepositorySummary repository,
            RepositoryStandardRuleDefinition rule) {
        var license = repository.license();
        if (license == null
                || license.analysisState() == null
                || license.analysisState() == AnalysisState.NOT_ANALYZED
                || license.analysisState() == AnalysisState.FAILED
                || license.presence() == null
                || license.presence() == LicensePresence.UNKNOWN) {
            return result(rule, RepositoryRuleEvaluationResult.UNKNOWN,
                    "License information is not available.", null);
        }
        boolean present = license.presence() == LicensePresence.PRESENT;
        return result(
                rule,
                present ? RepositoryRuleEvaluationResult.PASS : RepositoryRuleEvaluationResult.FAIL,
                present ? "A license is present." : "No license is present.",
                license.name() != null ? license.name() : license.presence().name());
    }

    private RepositoryRuleEvaluation evaluateActions(
            RepositorySummary repository,
            RepositoryStandardRuleDefinition rule) {
        var actions = repository.githubActions();
        if (actions == null
                || actions.analysisState() == null
                || actions.analysisState() == AnalysisState.NOT_ANALYZED
                || actions.analysisState() == AnalysisState.FAILED
                || actions.workflowsPresent() == null) {
            return result(rule, RepositoryRuleEvaluationResult.UNKNOWN,
                    "GitHub Actions workflow information is not available.", null);
        }
        return result(
                rule,
                actions.workflowsPresent()
                        ? RepositoryRuleEvaluationResult.PASS
                        : RepositoryRuleEvaluationResult.FAIL,
                actions.workflowsPresent()
                        ? "At least one GitHub Actions workflow is present."
                        : "No GitHub Actions workflow is present.",
                String.valueOf(actions.workflowCount()));
    }

    private RepositoryRuleEvaluation evaluateRelease(
            RepositorySummary repository,
            RepositoryStandardRuleDefinition rule) {
        var release = repository.release();
        if (release == null
                || release.analysisState() == null
                || release.analysisState() == AnalysisState.NOT_ANALYZED
                || release.analysisState() == AnalysisState.FAILED
                || release.releasePresent() == null) {
            return result(rule, RepositoryRuleEvaluationResult.UNKNOWN,
                    "Published release information is not available.", null);
        }
        return result(
                rule,
                release.releasePresent()
                        ? RepositoryRuleEvaluationResult.PASS
                        : RepositoryRuleEvaluationResult.FAIL,
                release.releasePresent()
                        ? "A published release is present."
                        : "No published release is present.",
                release.releasePresent() ? release.latestReleaseTag() : "none");
    }

    private RepositoryRuleEvaluation evaluateRequiredTopic(
            RepositorySummary repository,
            RepositoryStandardRuleDefinition rule) {
        String requiredTopic = stringParameter(rule.parameters(), "topic");
        if (requiredTopic == null) {
            return result(rule, RepositoryRuleEvaluationResult.NOT_APPLICABLE,
                    "Rule parameter 'topic' is missing.", null);
        }
        boolean present = repository.topics().stream()
                .anyMatch(topic -> topic.equalsIgnoreCase(requiredTopic));
        return result(
                rule,
                present ? RepositoryRuleEvaluationResult.PASS : RepositoryRuleEvaluationResult.FAIL,
                present ? "Required topic is present." : "Required topic is missing.",
                String.join(",", repository.topics()));
    }

    private RepositoryRuleEvaluation evaluateMaximumInactivity(
            RepositorySummary repository,
            RepositoryStandardRuleDefinition rule,
            Instant now) {
        Integer days = integerParameter(rule.parameters(), "days");
        if (days == null || days < 0) {
            return result(rule, RepositoryRuleEvaluationResult.NOT_APPLICABLE,
                    "Rule parameter 'days' is missing or invalid.", null);
        }
        Instant activityAt = repository.activity() == null
                ? null
                : repository.activity().pushedAt() != null
                        ? repository.activity().pushedAt()
                        : repository.activity().updatedAt();
        if (activityAt == null) {
            return result(rule, RepositoryRuleEvaluationResult.UNKNOWN,
                    "Repository activity timestamp is not available.", null);
        }
        long inactiveDays = Math.max(0, Duration.between(activityAt, now).toDays());
        return result(
                rule,
                inactiveDays <= days
                        ? RepositoryRuleEvaluationResult.PASS
                        : RepositoryRuleEvaluationResult.FAIL,
                inactiveDays <= days
                        ? "Repository activity is within the configured inactivity limit."
                        : "Repository has exceeded the configured inactivity limit.",
                inactiveDays + " days");
    }

    private RepositoryRuleEvaluation result(
            RepositoryStandardRuleDefinition rule,
            RepositoryRuleEvaluationResult result,
            String reason,
            String observed) {
        return new RepositoryRuleEvaluation(
                rule.ruleKey(),
                rule.ruleType(),
                rule.severity(),
                result,
                reason,
                observed);
    }

    private String stringParameter(Map<String, Object> parameters, String key) {
        if (parameters == null) {
            return null;
        }
        Object value = parameters.get(key);
        return value instanceof String string && !string.isBlank() ? string : null;
    }

    private Integer integerParameter(Map<String, Object> parameters, String key) {
        if (parameters == null) {
            return null;
        }
        Object value = parameters.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String string) {
            try {
                return Integer.parseInt(string);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
