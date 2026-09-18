package info.isaksson.erland.repofleet.github.webhook;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "github_webhook_delivery")
public class GitHubWebhookDelivery extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "delivery_id", nullable = false, unique = true)
    public String deliveryId;

    @Column(name = "event_type", nullable = false)
    public String eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_support", nullable = false)
    public GitHubWebhookEventSupport eventSupport;

    @Column(name = "received_at", nullable = false)
    public Instant receivedAt;
}
