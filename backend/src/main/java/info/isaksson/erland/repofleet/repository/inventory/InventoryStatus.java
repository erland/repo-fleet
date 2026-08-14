package info.isaksson.erland.repofleet.repository.inventory;

import java.time.Instant;

public record InventoryStatus(
        InventoryRefreshState state,
        Instant lastAttemptAt,
        Instant lastSuccessfulRefreshAt,
        String errorMessage,
        int repositoryCount) {
}
