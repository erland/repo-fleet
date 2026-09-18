package info.isaksson.erland.repofleet.standards;

import java.util.List;
import java.util.Map;

public record ComplianceRuleDetail(
        String ruleKey,
        String ruleName,
        String description,
        RepositoryRuleType ruleType,
        RepositoryRuleSeverity severity,
        RepositoryRuleScope scope,
        Map<String, Object> parameters,
        List<String> groups,
        Map<RepositoryRuleEvaluationResult, Long> resultCounts,
        List<ComplianceRuleAffectedRepository> affectedRepositories) {

    public ComplianceRuleDetail {
        groups = groups == null ? List.of() : List.copyOf(groups);
        affectedRepositories = affectedRepositories == null
                ? List.of()
                : List.copyOf(affectedRepositories);
    }
}

record ComplianceRuleAffectedRepository(
        long githubRepositoryId,
        String fullName,
        RepositoryRuleEvaluationResult result,
        String reason,
        String observedValue) {
}
