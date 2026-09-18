package info.isaksson.erland.repofleet.repository.persistence;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "repository_refresh_run")
public class RepositoryRefreshRun extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "trigger_type", nullable = false)
    public String triggerType;

    @Column(name = "started_at", nullable = false)
    public Instant startedAt;

    @Column(name = "completed_at")
    public Instant completedAt;

    @Column(name = "final_state", nullable = false)
    public String finalState;

    @Column(name = "discovered_count", nullable = false)
    public int discoveredCount;

    @Column(name = "processed_count", nullable = false)
    public int processedCount;

    @Column(name = "successful_count", nullable = false)
    public int successfulCount;

    @Column(name = "error_count", nullable = false)
    public int errorCount;

    @Column(name = "reused_count", nullable = false)
    public int reusedCount;

    @Column(name = "scheduled_count", nullable = false)
    public int scheduledCount;

    @Column(name = "failed_repository_summary", columnDefinition = "text")
    public String failedRepositorySummary;

    @Column(name = "rate_limit_remaining")
    public Integer rateLimitRemaining;

    @Column(name = "rate_limit_reset_at")
    public Instant rateLimitResetAt;
}
