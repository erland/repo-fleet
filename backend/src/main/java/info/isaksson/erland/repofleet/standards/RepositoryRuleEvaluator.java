package info.isaksson.erland.repofleet.standards;

import info.isaksson.erland.repofleet.repository.api.AnalysisState;
import info.isaksson.erland.repofleet.repository.api.LicensePresence;
import info.isaksson.erland.repofleet.repository.api.RepositorySummary;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;

@ApplicationScoped
public class RepositoryRuleEvaluator {

    public RepositoryRuleEvaluation evaluate(
            RepositorySummary repository,
            RepositoryStandardRuleDefinition rule,
            Instant now) {
        if (rule == null) {
            throw new IllegalArgumentException("rule must not be null");
        }
        if (repository == null) {
            throw new IllegalArgumentException("repository must not be null");
        }

        return switch (rule.ruleType()) {
            case LICENSE_REQUIRED -> evaluateLicense(repository, rule);
            case ACTIONS_WORKFLOW_REQUIRED -> evaluateActions(repository, rule);
            case PUBLISHED_RELEASE_REQUIRED -> evaluateRelease(repository, rule);
            case REQUIRED_TOPIC -> evaluateRequiredTopic(repository, rule);
            case MAXIMUM_INACTIVITY_AGE -> evaluateMaximumInactivity(repository, rule, now);
            case README_REQUIRED -> unknown(
                    rule,
                    "README presence is not reliably available in the current inventory.",
                    null);
        };
    }

    private RepositoryRuleEvaluation evaluateLicense(
            RepositorySummary repository,
            RepositoryStandardRuleDefinition rule) {
        var license = repository.license();
        if (license == null || license.analysisState() != AnalysisState.COMPLETE) {
            return unknown(rule, "License analysis is incomplete.", null);
        }
        if (license.presence() == null || license.presence() == LicensePresence.UNKNOWN) {
            return unknown(rule, "License presence is unknown.", null);
        }
        if (license.presence() == LicensePresence.MISSING) {
            return fail(rule, "Repository does not contain a license.", "MISSING");
        }
        return pass(rule, "Repository contains a license.", license.name() == null ? "PRESENT" : license.name());
    }

    private RepositoryRuleEvaluation evaluateActions(
            RepositorySummary repository,
            RepositoryStandardRuleDefinition rule) {
        var actions = repository.githubActions();
        if (actions == null || actions.analysisState() != AnalysisState.COMPLETE
                || actions.workflowsPresent() == null) {
            return unknown(rule, "GitHub Actions analysis is incomplete.", null);
        }
        if (!actions.workflowsPresent()) {
            return fail(rule, "Repository has no GitHub Actions workflows.", "0 workflows");
        }
        return pass(
                rule,
                "Repository has GitHub Actions workflows.",
                actions.workflowCount() == null ? "present" : actions.workflowCount() + " workflows");
    }

    private RepositoryRuleEvaluation evaluateRelease(
            RepositorySummary repository,
            RepositoryStandardRuleDefinition rule) {
        var release = repository.release();
        if (release == null || release.analysisState() != AnalysisState.COMPLETE
                || release.releasePresent() == null) {
            return unknown(rule, "Release analysis is incomplete.", null);
        }
        if (!release.releasePresent()) {
            return fail(rule, "Repository has no published release.", "none");
        }
        return pass(
                rule,
                "Repository has a published release.",
                release.latestReleaseTag() == null ? "published release present" : release.latestReleaseTag());
    }

    private RepositoryRuleEvaluation evaluateRequiredTopic(
            RepositorySummary repository,
            RepositoryStandardRuleDefinition rule) {
        String required = stringParameter(rule.parameters(), "topic");
        if (required == null) {
            return notApplicable(rule, "Rule is missing required parameter 'topic'.", null);
        }
        boolean present = repository.topics().stream()
                .anyMatch(topic -> topic.equalsIgnoreCase(required));
        if (present) {
            return pass(rule, "Repository contains required topic.", required);
        }
        return fail(
                rule,
                "Repository does not contain required topic.",
                repository.topics().isEmpty() ? "no topics" : String.join(", ", repository.topics()));
    }

    private RepositoryRuleEvaluation evaluateMaximumInactivity(
            RepositorySummary repository,
            RepositoryStandardRuleDefinition rule,
            Instant now) {
        Integer days = integerParameter(rule.parameters(), "days");
        if (days == null || days < 0) {
            return notApplicable(rule, "Rule is missing a valid non-negative 'days' parameter.", null);
        }
        Instant lastActivity = repository.activity() == null
                ? null
                : repository.activity().pushedAt() != null
                        ? repository.activity().pushedAt()
                        : repository.activity().updatedAt();
        if (lastActivity == null) {
            return unknown(rule, "Repository activity timestamp is unknown.", null);
        }

        long inactiveDays = Math.max(0, Duration.between(lastActivity, now).toDays());
        if (inactiveDays <= days) {
            return pass(
                    rule,
                    "Repository activity is within the configured inactivity limit.",
                    inactiveDays + " days");
        }
        return fail(
                rule,
                "Repository exceeds the configured inactivity limit.",
                inactiveDays + " days");
    }

    private String stringParameter(Map<String, Object> parameters, String key) {
        if (parameters == null) {
            return null;
        }
        Object value = parameters.get(key);
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }

    private Integer integerParameter(Map<String, Object> parameters, String key) {
        if (parameters == null) {
            return null;
        }
        Object value = parameters.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text) {
            try {
                return Integer.valueOf(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private RepositoryRuleEvaluation pass(
            RepositoryStandardRuleDefinition rule,
            String reason,
            String observedValue) {
        return evaluation(rule, RepositoryRuleEvaluationResult.PASS, reason, observedValue);
    }

    private RepositoryRuleEvaluation fail(
            RepositoryStandardRuleDefinition rule,
            String reason,
            String observedValue) {
        return evaluation(rule, RepositoryRuleEvaluationResult.FAIL, reason, observedValue);
    }

    private RepositoryRuleEvaluation unknown(
            RepositoryStandardRuleDefinition rule,
            String reason,
            String observedValue) {
        return evaluation(rule, RepositoryRuleEvaluationResult.UNKNOWN, reason, observedValue);
    }

    private RepositoryRuleEvaluation notApplicable(
            RepositoryStandardRuleDefinition rule,
            String reason,
            String observedValue) {
        return evaluation(rule, RepositoryRuleEvaluationResult.NOT_APPLICABLE, reason, observedValue);
    }

    private RepositoryRuleEvaluation evaluation(
            RepositoryStandardRuleDefinition rule,
            RepositoryRuleEvaluationResult result,
            String reason,
            String observedValue) {
        return new RepositoryRuleEvaluation(
                rule.ruleKey(),
                rule.ruleType(),
                rule.severity(),
                result,
                reason,
                observedValue);
    }
}
