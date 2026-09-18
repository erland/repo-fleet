package info.isaksson.erland.repofleet.repository.refresh;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import info.isaksson.erland.repofleet.repository.inventory.InMemoryRepositoryInventoryService;
import org.junit.jupiter.api.Test;

class RepositoryConsistencyRefreshSchedulerTest {

    @Test
    void delegatesConsistencyRunToIncrementalInventoryRefresh() {
        InMemoryRepositoryInventoryService inventory =
                mock(InMemoryRepositoryInventoryService.class);

        RepositoryConsistencyRefreshScheduler scheduler =
                new RepositoryConsistencyRefreshScheduler(
                        inventory,
                        false,
                        24);

        scheduler.runOnce();

        verify(inventory).startScheduledConsistencyRefresh();
    }
}
