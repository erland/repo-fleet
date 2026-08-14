package info.isaksson.erland.repofleet.github.api;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/github/connection")
@Produces(MediaType.APPLICATION_JSON)
public class GitHubConnectionResource {

    private final GitHubConnectionService connectionService;

    @Inject
    public GitHubConnectionResource(GitHubConnectionService connectionService) {
        this.connectionService = connectionService;
    }

    @GET
    public GitHubConnectionStatus connection() {
        return connectionService.check();
    }
}
