package info.isaksson.erland.repofleet.standards;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/compliance/rules/{ruleKey}")
@Produces(MediaType.APPLICATION_JSON)
public class ComplianceRuleDetailResource {

    private final ComplianceRuleDetailService details;

    @Inject
    public ComplianceRuleDetailResource(ComplianceRuleDetailService details) {
        this.details = details;
    }

    @GET
    public ComplianceRuleDetail detail(@PathParam("ruleKey") String ruleKey) {
        ComplianceRuleDetail detail = details.detail(ruleKey);
        if (detail == null) {
            throw new NotFoundException("Rule not found");
        }
        return detail;
    }
}
