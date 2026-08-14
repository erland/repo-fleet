package info.isaksson.erland.repofleet.repository.api;

import java.time.Instant;

public record ActivityStatus(
    Instant pushedAt,
    Instant updatedAt
) {
}
