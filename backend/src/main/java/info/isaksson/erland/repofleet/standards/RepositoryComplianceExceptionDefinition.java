package info.isaksson.erland.repofleet.standards;

import java.time.Instant;

public record RepositoryComplianceExceptionDefinition(
        long githubRepositoryId,
        String ruleKey,
        String reason,
        Instant expiresAt,
        RepositoryComplianceExceptionState state,
        Instant createdAt,
        Instant updatedAt) {
}
