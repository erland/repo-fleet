package info.isaksson.erland.repofleet.standards;

import info.isaksson.erland.repofleet.repository.inventory.RepositoryInventoryService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Path("/api/standards/repositories/{repositoryId}/compliance")
@Produces(MediaType.APPLICATION_JSON)
public class RepositoryComplianceResource {

    private final RepositoryInventoryService inventory;
    private final RepositoryComplianceEvaluationService compliance;

    @Inject
    public RepositoryComplianceResource(
            RepositoryInventoryService inventory,
            RepositoryComplianceEvaluationService compliance) {
        this.inventory = inventory;
        this.compliance = compliance;
    }

    @GET
    public List<RepositoryRuleEvaluation> list(@PathParam("repositoryId") long repositoryId) {
        var repository = inventory.listRepositories().stream()
                .filter(item -> item.id() == repositoryId)
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Repository not found"));
        return compliance.evaluate(repository);
    }
}
