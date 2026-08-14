package info.isaksson.erland.repofleet.github.client;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.Map;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Produces(MediaType.APPLICATION_JSON)
@RegisterRestClient(configKey = "github-api")
public interface GitHubRepositoryMetadataClient {

    @GET
    @Path("/repos/{owner}/{repository}/topics")
    GitHubTopicsResponse getTopics(
        @PathParam("owner") String owner,
        @PathParam("repository") String repository,
        @HeaderParam("Authorization") String authorization,
        @HeaderParam("Accept") String accept,
        @HeaderParam("X-GitHub-Api-Version") String apiVersion
    );

    @GET
    @Path("/repos/{owner}/{repository}/languages")
    Map<String, Long> getLanguages(
        @PathParam("owner") String owner,
        @PathParam("repository") String repository,
        @HeaderParam("Authorization") String authorization,
        @HeaderParam("Accept") String accept,
        @HeaderParam("X-GitHub-Api-Version") String apiVersion
    );
}
