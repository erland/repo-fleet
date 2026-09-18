package info.isaksson.erland.repofleet.standards;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Path("/api/standards/rules")
@Produces(MediaType.APPLICATION_JSON)
public class RepositoryStandardRuleResource {

    private final RepositoryStandardRuleService rules;

    @Inject
    public RepositoryStandardRuleResource(RepositoryStandardRuleService rules) {
        this.rules = rules;
    }

    @GET
    public List<RepositoryStandardRuleDefinition> list() {
        return rules.list();
    }
}
