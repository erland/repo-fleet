package info.isaksson.erland.repofleet.github.webhook;

public record GitHubWebhookReceipt(
        String deliveryId,
        String eventType,
        GitHubWebhookEventSupport eventSupport,
        boolean duplicate) {
}
