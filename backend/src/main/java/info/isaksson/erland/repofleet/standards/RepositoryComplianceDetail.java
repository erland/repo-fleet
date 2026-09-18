package info.isaksson.erland.repofleet.standards;

import java.time.Instant;

public record RepositoryComplianceDetail(
        String ruleKey,
        String ruleName,
        RepositoryRuleType ruleType,
        RepositoryRuleSeverity severity,
        RepositoryRuleEvaluationResult result,
        String reason,
        String observedValue,
        Instant evaluatedAt,
        boolean acceptedDeviation,
        String exceptionReason,
        Instant exceptionExpiresAt) {
}
