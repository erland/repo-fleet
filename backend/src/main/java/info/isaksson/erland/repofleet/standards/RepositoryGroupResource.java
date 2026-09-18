package info.isaksson.erland.repofleet.standards;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Path("/api/standards/groups")
@Produces(MediaType.APPLICATION_JSON)
public class RepositoryGroupResource {

    private final RepositoryGroupService groups;

    @Inject
    public RepositoryGroupResource(RepositoryGroupService groups) {
        this.groups = groups;
    }

    @GET
    public List<RepositoryGroupDefinition> list() {
        return groups.list();
    }
}
