package info.isaksson.erland.repofleet.api;

import info.isaksson.erland.repofleet.repository.inventory.InventoryStatus;
import info.isaksson.erland.repofleet.repository.inventory.RepositoryInventoryService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/inventory")
@Produces(MediaType.APPLICATION_JSON)
public class InventoryResource {

    private final RepositoryInventoryService inventoryService;

    @Inject
    public InventoryResource(RepositoryInventoryService inventoryService) {
        this.inventoryService = inventoryService;
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
}
