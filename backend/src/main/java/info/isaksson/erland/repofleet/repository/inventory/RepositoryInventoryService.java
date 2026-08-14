package info.isaksson.erland.repofleet.repository.inventory;

import info.isaksson.erland.repofleet.repository.api.RepositorySummary;
import java.util.List;

public interface RepositoryInventoryService {

    List<RepositorySummary> listRepositories();

    InventoryStatus getStatus();

    InventoryStatus refresh();

    InventoryStatus startRefresh();
}
