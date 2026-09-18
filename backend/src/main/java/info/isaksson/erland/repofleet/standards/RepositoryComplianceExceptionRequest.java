package info.isaksson.erland.repofleet.standards;

import java.time.Instant;

public record RepositoryComplianceExceptionRequest(
        String reason,
        Instant expiresAt) {
}
