package info.isaksson.erland.repofleet.standards;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/compliance/summary")
@Produces(MediaType.APPLICATION_JSON)
public class RepositoryComplianceSummaryResource {

    private final RepositoryComplianceSummaryService summaries;

    @Inject
    public RepositoryComplianceSummaryResource(
            RepositoryComplianceSummaryService summaries) {
        this.summaries = summaries;
    }

    @GET
    public CompliancePortfolioSummary summary() {
        return summaries.summarize();
    }
}
