package info.isaksson.erland.repofleet.repository.api;

import info.isaksson.erland.repofleet.repository.inventory.RepositoryInventoryService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

@Path("/api/repositories")
@Produces(MediaType.APPLICATION_JSON)
public class RepositoryResource {

    private final RepositoryInventoryService inventoryService;

    @Inject
    public RepositoryResource(RepositoryInventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GET
    public List<RepositorySummary> repositories() {
        return inventoryService.listRepositories();
    }
}
