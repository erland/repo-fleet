package info.isaksson.erland.repofleet.standards;

public record RepositoryRuleEvaluation(
        String ruleKey,
        RepositoryRuleType ruleType,
        RepositoryRuleSeverity severity,
        RepositoryRuleEvaluationResult result,
        String reason,
        String observedValue) {
}
