package info.isaksson.erland.repofleet.github.client;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Produces(MediaType.APPLICATION_JSON)
@RegisterRestClient(configKey = "github-api")
public interface GitHubAppClient {

    @GET
    @Path("/app")
    GitHubAppResponse getAuthenticatedApp(
        @HeaderParam("Authorization") String authorization,
        @HeaderParam("Accept") String accept,
        @HeaderParam("X-GitHub-Api-Version") String apiVersion
    );

    @POST
    @Path("/app/installations/{installationId}/access_tokens")
    InstallationTokenResponse createInstallationToken(
        @PathParam("installationId") long installationId,
        @HeaderParam("Authorization") String authorization,
        @HeaderParam("Accept") String accept,
        @HeaderParam("X-GitHub-Api-Version") String apiVersion
    );
}
