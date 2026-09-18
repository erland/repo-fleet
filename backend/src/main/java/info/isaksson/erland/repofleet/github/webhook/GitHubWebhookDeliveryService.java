package info.isaksson.erland.repofleet.github.webhook;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.util.Set;

@ApplicationScoped
public class GitHubWebhookDeliveryService {
    private static final Set<String> SUPPORTED_EVENTS = Set.of(
            "repository",
            "push",
            "release",
            "workflow_run",
            "installation_repositories");

    private final Clock clock;

    public GitHubWebhookDeliveryService() {
        this(Clock.systemUTC());
    }

    GitHubWebhookDeliveryService(Clock clock) {
        this.clock = clock;
    }

    @Transactional
    public GitHubWebhookReceipt record(String deliveryId, String eventType) {
        GitHubWebhookDelivery existing = GitHubWebhookDelivery.find(
                        "deliveryId",
                        deliveryId)
                .firstResultOptional()
                .map(GitHubWebhookDelivery.class::cast)
                .orElse(null);

        if (existing != null) {
            return new GitHubWebhookReceipt(
                    existing.deliveryId,
                    existing.eventType,
                    existing.eventSupport,
                    true);
        }

        GitHubWebhookDelivery entity = new GitHubWebhookDelivery();
        entity.deliveryId = deliveryId;
        entity.eventType = eventType;
        entity.eventSupport = SUPPORTED_EVENTS.contains(eventType)
                ? GitHubWebhookEventSupport.SUPPORTED
                : GitHubWebhookEventSupport.UNSUPPORTED;
        entity.receivedAt = clock.instant();
        entity.persist();

        return new GitHubWebhookReceipt(
                entity.deliveryId,
                entity.eventType,
                entity.eventSupport,
                false);
    }
}
