package info.isaksson.erland.repofleet.standards;

import info.isaksson.erland.repofleet.repository.persistence.RepositoryIdentityRepository;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Path("/api/compliance/repositories/{repositoryId}")
@Produces(MediaType.APPLICATION_JSON)
public class RepositoryComplianceDetailResource {

    private final RepositoryComplianceResultService results;
    private final RepositoryIdentityRepository identities;

    @Inject
    public RepositoryComplianceDetailResource(
            RepositoryComplianceResultService results,
            RepositoryIdentityRepository identities) {
        this.results = results;
        this.identities = identities;
    }

    @GET
    public List<RepositoryComplianceDetail> detail(
            @PathParam("repositoryId") long repositoryId) {
        if (identities.findByGitHubRepositoryId(repositoryId).isEmpty()) {
            throw new NotFoundException("Repository not found");
        }

        return results.listForRepository(repositoryId).stream()
                .map(stored -> {
                    RepositoryStandardRule rule = RepositoryStandardRule.find(
                                    "ruleKey",
                                    stored.evaluation().ruleKey())
                            .firstResultOptional()
                            .map(RepositoryStandardRule.class::cast)
                            .orElseThrow(() -> new IllegalStateException(
                                    "Stored compliance result references missing rule: "
                                            + stored.evaluation().ruleKey()));
                    return new RepositoryComplianceDetail(
                            stored.evaluation().ruleKey(),
                            rule.name,
                            stored.evaluation().ruleType(),
                            stored.evaluation().severity(),
                            stored.evaluation().result(),
                            stored.evaluation().reason(),
                            stored.evaluation().observedValue(),
                            stored.evaluatedAt());
                })
                .sorted(java.util.Comparator
                        .comparing(RepositoryComplianceDetail::severity)
                        .thenComparing(RepositoryComplianceDetail::ruleName))
                .toList();
    }
}
