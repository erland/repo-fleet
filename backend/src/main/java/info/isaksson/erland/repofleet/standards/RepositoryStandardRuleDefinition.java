package info.isaksson.erland.repofleet.standards;

import java.time.Instant;
import java.util.Map;

public record RepositoryStandardRuleDefinition(
        String ruleKey,
        RepositoryRuleType ruleType,
        String name,
        String description,
        RepositoryRuleSeverity severity,
        boolean enabled,
        Map<String, Object> parameters,
        RepositoryRuleScope scope,
        Instant createdAt,
        Instant updatedAt) {
}
