package info.isaksson.erland.repofleet.repository.inventory;

import info.isaksson.erland.repofleet.repository.model.RepositorySummary;
import java.util.List;

public record RepositoryInventorySnapshot(
        List<RepositorySummary> repositories,
        InventoryStatus status) {
}
