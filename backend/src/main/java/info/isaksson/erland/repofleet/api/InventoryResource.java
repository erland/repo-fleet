package info.isaksson.erland.repofleet.api;

import info.isaksson.erland.repofleet.repository.inventory.InventoryStatus;
import info.isaksson.erland.repofleet.repository.inventory.RepositoryInventoryService;
import info.isaksson.erland.repofleet.repository.persistence.RepositoryRefreshHistoryService;
import info.isaksson.erland.repofleet.repository.persistence.RepositoryRefreshRunSummary;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Path("/api/inventory")
@Produces(MediaType.APPLICATION_JSON)
public class InventoryResource {

    private final RepositoryInventoryService inventoryService;
    private final RepositoryRefreshHistoryService refreshHistoryService;

    @Inject
    public InventoryResource(
            RepositoryInventoryService inventoryService,
            RepositoryRefreshHistoryService refreshHistoryService) {
        this.inventoryService = inventoryService;
        this.refreshHistoryService = refreshHistoryService;
    }

    @GET
    @Path("/status")
    public InventoryStatus status() {
        return inventoryService.getStatus();
    }

    @POST
    @Path("/refresh")
    public InventoryStatus refresh() {
        return inventoryService.startRefresh();
    }

    @POST
    @Path("/refresh/full")
    public InventoryStatus fullRefresh() {
        return inventoryService.startFullRefresh();
    }

    @GET
    @Path("/history")
    public List<RepositoryRefreshRunSummary> history(@QueryParam("limit") Integer limit) {
        return refreshHistoryService.recentRuns(limit == null ? 20 : limit);
    }
}
