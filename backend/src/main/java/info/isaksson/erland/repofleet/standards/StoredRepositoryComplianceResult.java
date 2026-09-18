package info.isaksson.erland.repofleet.standards;

import java.time.Instant;

public record StoredRepositoryComplianceResult(
        long githubRepositoryId,
        RepositoryRuleEvaluation evaluation,
        Instant evaluatedAt,
        Instant sourceUpdatedAt,
        Instant ruleUpdatedAt) {
}
