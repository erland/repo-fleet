package info.isaksson.erland.repofleet.standards;

import java.util.List;
import java.util.Map;

public record CompliancePortfolioSummary(
        long repositoryCount,
        long evaluatedRuleCount,
        long acceptedDeviationCount,
        Map<RepositoryRuleEvaluationResult, Long> resultCounts,
        Map<RepositoryRuleSeverity, Map<RepositoryRuleEvaluationResult, Long>> severityResultCounts,
        List<ComplianceGroupSummary> groups,
        List<ComplianceRepositoryFailureSummary> repositoriesWithMostRequiredFailures,
        List<ComplianceRuleSummary> rules) {
}

record ComplianceGroupSummary(
        String groupKey,
        String groupName,
        long repositoryCount,
        long acceptedDeviationCount,
        Map<RepositoryRuleEvaluationResult, Long> resultCounts) {
}

record ComplianceRepositoryFailureSummary(
        long githubRepositoryId,
        String fullName,
        long requiredFailureCount) {
}

record ComplianceRuleSummary(
        String ruleKey,
        String ruleName,
        RepositoryRuleSeverity severity,
        long acceptedDeviationCount,
        Map<RepositoryRuleEvaluationResult, Long> resultCounts) {
}
