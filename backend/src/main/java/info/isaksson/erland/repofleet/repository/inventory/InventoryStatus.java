package info.isaksson.erland.repofleet.repository.inventory;

import java.time.Instant;

public record InventoryStatus(
        InventoryRefreshState state,
        Instant lastAttemptAt,
        Instant lastSuccessfulRefreshAt,
        Instant completedAt,
        String errorMessage,
        int repositoryCount,
        int totalCount,
        int processedCount,
        int successfulCount,
        int errorCount,
        String currentRepository) {

    public static InventoryStatus notStarted() {
        return new InventoryStatus(
                InventoryRefreshState.NOT_STARTED,
                null,
                null,
                null,
                null,
                0,
                0,
                0,
                0,
                0,
                null);
    }

    public boolean running() {
        return state == InventoryRefreshState.RUNNING;
    }
}
