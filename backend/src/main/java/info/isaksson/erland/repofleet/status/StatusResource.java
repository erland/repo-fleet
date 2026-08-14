package info.isaksson.erland.repofleet.status;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/status")
@Produces(MediaType.APPLICATION_JSON)
public class StatusResource {

    @GET
    public StatusResponse status() {
        return new StatusResponse("repo-fleet-backend", "UP");
    }
}
