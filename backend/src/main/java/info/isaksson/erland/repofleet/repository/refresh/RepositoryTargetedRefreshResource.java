package info.isaksson.erland.repofleet.repository.refresh;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.Clock;

@Path("/api/repositories/{repositoryId}/targeted-refresh")
@Produces(MediaType.APPLICATION_JSON)
public class RepositoryTargetedRefreshResource {

    private final RepositoryRefreshQueueService queue;
    private final Clock clock;

    @Inject
    public RepositoryTargetedRefreshResource(RepositoryRefreshQueueService queue) {
        this(queue, Clock.systemUTC());
    }

    RepositoryTargetedRefreshResource(
            RepositoryRefreshQueueService queue,
            Clock clock) {
        this.queue = queue;
        this.clock = clock;
    }

    @POST
    public Response enqueue(@PathParam("repositoryId") long repositoryId) {
        try {
            return Response.accepted(
                            queue.enqueue(repositoryId, "MANUAL_SINGLE_REPOSITORY", clock.instant()))
                    .build();
        } catch (IllegalArgumentException exception) {
            throw new NotFoundException(exception.getMessage());
        }
    }

    @GET
    @Path("/jobs/{jobId}")
    public RepositoryRefreshJobView job(@PathParam("jobId") long jobId) {
        RepositoryRefreshJobView job = queue.get(jobId);
        if (job == null) throw new NotFoundException("Refresh job not found");
        return job;
    }
}
