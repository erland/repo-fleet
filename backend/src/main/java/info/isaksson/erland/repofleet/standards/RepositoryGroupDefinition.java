package info.isaksson.erland.repofleet.standards;

import java.time.Instant;

public record RepositoryGroupDefinition(
        String groupKey,
        String name,
        String description,
        RepositoryGroupSelector selector,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt) {
}
