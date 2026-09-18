package info.isaksson.erland.repofleet.standards;

import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.Clock;
import java.util.List;

@Path("/api/compliance/repositories/{repositoryId}/exceptions")
@Produces(MediaType.APPLICATION_JSON)
public class RepositoryComplianceExceptionResource {

    private final RepositoryComplianceExceptionService exceptions;
    private final Clock clock;

    @Inject
    public RepositoryComplianceExceptionResource(
            RepositoryComplianceExceptionService exceptions) {
        this(exceptions, Clock.systemUTC());
    }

    RepositoryComplianceExceptionResource(
            RepositoryComplianceExceptionService exceptions,
            Clock clock) {
        this.exceptions = exceptions;
        this.clock = clock;
    }

    @GET
    public List<RepositoryComplianceExceptionDefinition> list(
            @PathParam("repositoryId") long repositoryId) {
        return exceptions.listForRepository(repositoryId);
    }

    @POST
    @jakarta.ws.rs.Consumes(MediaType.APPLICATION_JSON)
    @Path("/{ruleKey}")
    public RepositoryComplianceExceptionDefinition save(
            @PathParam("repositoryId") long repositoryId,
            @PathParam("ruleKey") String ruleKey,
            RepositoryComplianceExceptionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request body is required");
        }
        return exceptions.save(
                repositoryId,
                ruleKey,
                request.reason(),
                request.expiresAt(),
                clock.instant());
    }

    @POST
    @Path("/{ruleKey}/expire")
    public Response expire(
            @PathParam("repositoryId") long repositoryId,
            @PathParam("ruleKey") String ruleKey) {
        if (!exceptions.expire(repositoryId, ruleKey, clock.instant())) {
            throw new NotFoundException("Exception not found");
        }
        return Response.noContent().build();
    }

    @DELETE
    @Path("/{ruleKey}")
    public Response remove(
            @PathParam("repositoryId") long repositoryId,
            @PathParam("ruleKey") String ruleKey) {
        if (!exceptions.remove(repositoryId, ruleKey)) {
            throw new NotFoundException("Exception not found");
        }
        return Response.noContent().build();
    }
}
