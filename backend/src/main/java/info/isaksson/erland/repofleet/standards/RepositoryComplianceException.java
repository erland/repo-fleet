package info.isaksson.erland.repofleet.standards;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "repository_compliance_exception")
public class RepositoryComplianceException extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "github_repository_id", nullable = false)
    public long githubRepositoryId;

    @Column(name = "rule_key", nullable = false)
    public String ruleKey;

    @Column(nullable = false, columnDefinition = "text")
    public String reason;

    @Column(name = "expires_at")
    public Instant expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public RepositoryComplianceExceptionState state;

    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;
}
