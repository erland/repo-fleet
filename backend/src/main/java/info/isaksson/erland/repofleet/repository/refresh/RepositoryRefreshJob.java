package info.isaksson.erland.repofleet.repository.refresh;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "repository_refresh_job")
public class RepositoryRefreshJob extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "github_repository_id", nullable = false)
    public long githubRepositoryId;

    @Column(name = "trigger_type", nullable = false)
    public String triggerType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public RepositoryRefreshJobState state;

    @Column(nullable = false)
    public int attempts;

    @Column(name = "max_attempts", nullable = false)
    public int maxAttempts;

    @Column(name = "next_attempt_at", nullable = false)
    public Instant nextAttemptAt;

    @Column(name = "last_error", columnDefinition = "text")
    public String lastError;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;

    @Column(name = "completed_at")
    public Instant completedAt;
}
