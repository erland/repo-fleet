package info.isaksson.erland.repofleet.standards;

import info.isaksson.erland.repofleet.repository.inventory.RepositoryInventoryService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Path("/api/standards/repositories/{repositoryId}/rules")
@Produces(MediaType.APPLICATION_JSON)
public class RepositoryApplicableRulesResource {

    private final RepositoryInventoryService inventory;
    private final RepositoryRuleAssignmentService assignments;

    @Inject
    public RepositoryApplicableRulesResource(
            RepositoryInventoryService inventory,
            RepositoryRuleAssignmentService assignments) {
        this.inventory = inventory;
        this.assignments = assignments;
    }

    @GET
    public List<ApplicableRepositoryRule> list(@PathParam("repositoryId") long repositoryId) {
        var repository = inventory.listRepositories().stream()
                .filter(item -> item.id() == repositoryId)
                .findFirst()
                .orElseThrow(() -> new jakarta.ws.rs.NotFoundException("Repository not found"));
        return assignments.applicableRules(repository);
    }
}
