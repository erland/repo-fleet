package info.isaksson.erland.repofleet.github.diagnostics;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "github_api_diagnostics")
public class GitHubApiDiagnostics extends PanacheEntityBase {
    @Id
    public Long id;

    @Column(name = "conditional_modified_count", nullable = false)
    public long conditionalModifiedCount;

    @Column(name = "conditional_not_modified_count", nullable = false)
    public long conditionalNotModifiedCount;

    @Column(name = "conditional_cached_fresh_count", nullable = false)
    public long conditionalCachedFreshCount;

    @Column(name = "rate_limit_remaining")
    public Integer rateLimitRemaining;

    @Column(name = "rate_limit_reset_at")
    public Instant rateLimitResetAt;

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;
}
