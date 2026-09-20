package info.isaksson.erland.repofleet.github.webhook;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
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

    private final EntityManager entityManager;
    private final Clock clock;
    private final GitHubWebhookEventProcessor processor;

    @Inject
    public GitHubWebhookDeliveryService(
            EntityManager entityManager,
            GitHubWebhookEventProcessor processor) {
        this(entityManager, processor, Clock.systemUTC());
    }

    GitHubWebhookDeliveryService(
            EntityManager entityManager,
            GitHubWebhookEventProcessor processor,
            Clock clock) {
        this.entityManager = entityManager;
        this.processor = processor;
        this.clock = clock;
    }

    @Transactional
    public GitHubWebhookReceipt record(
            String deliveryId,
            String eventType,
            String payload) {
        Instant receivedAt = clock.instant();
        GitHubWebhookEventSupport support = SUPPORTED_EVENTS.contains(eventType)
                ? GitHubWebhookEventSupport.SUPPORTED
                : GitHubWebhookEventSupport.UNSUPPORTED;

        int claimed = entityManager.createNativeQuery("""
                        INSERT INTO github_webhook_delivery
                            (delivery_id, event_type, event_support, received_at)
                        VALUES
                            (:deliveryId, :eventType, :eventSupport, :receivedAt)
                        ON CONFLICT (delivery_id) DO NOTHING
                        """)
                .setParameter("deliveryId", deliveryId)
                .setParameter("eventType", eventType)
                .setParameter("eventSupport", support.name())
                .setParameter("receivedAt", receivedAt)
                .executeUpdate();

        if (claimed == 0) {
            GitHubWebhookDelivery existing = GitHubWebhookDelivery.find(
                            "deliveryId",
                            deliveryId)
                    .firstResultOptional()
                    .map(GitHubWebhookDelivery.class::cast)
                    .orElseThrow(() -> new IllegalStateException(
                            "Webhook delivery claim exists but could not be loaded: " + deliveryId));
            return new GitHubWebhookReceipt(
                    existing.deliveryId,
                    existing.eventType,
                    existing.eventSupport,
                    true);
        }

        processor.process(eventType, payload, receivedAt);

        return new GitHubWebhookReceipt(
                deliveryId,
                eventType,
                support,
                false);
    }
}
