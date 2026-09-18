package info.isaksson.erland.repofleet.github.webhook;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/github/webhook")
@Produces(MediaType.APPLICATION_JSON)
public class GitHubWebhookResource {
    private final GitHubWebhookSignatureVerifier verifier;
    private final GitHubWebhookDeliveryService deliveries;

    @Inject
    public GitHubWebhookResource(
            GitHubWebhookSignatureVerifier verifier,
            GitHubWebhookDeliveryService deliveries) {
        this.verifier = verifier;
        this.deliveries = deliveries;
    }

    @POST
    @Consumes(MediaType.WILDCARD)
    public Response receive(
            @HeaderParam("X-Hub-Signature-256") String signature,
            @HeaderParam("X-GitHub-Delivery") String deliveryId,
            @HeaderParam("X-GitHub-Event") String eventType,
            String payload) {
        if (deliveryId == null || deliveryId.isBlank()
                || eventType == null || eventType.isBlank()
                || payload == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        if (!verifier.verify(payload, signature)) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        GitHubWebhookReceipt receipt = deliveries.record(deliveryId, eventType);
        return Response.ok(receipt).build();
    }
}
