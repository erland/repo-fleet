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
    private final RepositoryComplianceExceptionService exceptions;

    @Inject
    public RepositoryComplianceDetailResource(
            RepositoryComplianceResultService results,
            RepositoryIdentityRepository identities,
            RepositoryComplianceExceptionService exceptions) {
        this.results = results;
        this.identities = identities;
        this.exceptions = exceptions;
    }

    @GET
    public List<RepositoryComplianceDetail> detail(
            @PathParam("repositoryId") long repositoryId) {
        if (identities.findByGitHubRepositoryId(repositoryId).isEmpty()) {
            throw new NotFoundException("Repository not found");
        }

        java.util.Map<String, RepositoryComplianceExceptionDefinition> activeExceptions =
                exceptions.listForRepository(repositoryId).stream()
                        .filter(exception -> exception.state() == RepositoryComplianceExceptionState.ACTIVE)
                        .collect(java.util.stream.Collectors.toMap(
                                RepositoryComplianceExceptionDefinition::ruleKey,
                                exception -> exception));

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
                    RepositoryComplianceExceptionDefinition exception =
                            activeExceptions.get(stored.evaluation().ruleKey());
                    return new RepositoryComplianceDetail(
                            stored.evaluation().ruleKey(),
                            rule.name,
                            stored.evaluation().ruleType(),
                            stored.evaluation().severity(),
                            stored.evaluation().result(),
                            stored.evaluation().reason(),
                            stored.evaluation().observedValue(),
                            stored.evaluatedAt(),
                            exception != null,
                            exception == null ? null : exception.reason(),
                            exception == null ? null : exception.expiresAt());
                })
                .sorted(java.util.Comparator
                        .comparing(RepositoryComplianceDetail::severity)
                        .thenComparing(RepositoryComplianceDetail::ruleName))
                .toList();
    }
}
