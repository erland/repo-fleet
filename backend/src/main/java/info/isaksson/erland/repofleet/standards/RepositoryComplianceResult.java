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
@Table(name = "repository_compliance_result")
public class RepositoryComplianceResult extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "github_repository_id", nullable = false)
    public long githubRepositoryId;

    @Column(name = "rule_key", nullable = false)
    public String ruleKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public RepositoryRuleEvaluationResult result;

    @Column(nullable = false, columnDefinition = "text")
    public String reason;

    @Column(name = "observed_value", columnDefinition = "text")
    public String observedValue;

    @Column(name = "evaluated_at", nullable = false)
    public Instant evaluatedAt;

    @Column(name = "source_updated_at")
    public Instant sourceUpdatedAt;

    @Column(name = "rule_updated_at", nullable = false)
    public Instant ruleUpdatedAt;
}
