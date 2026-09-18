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
@Table(name = "repository_standard_rule")
public class RepositoryStandardRule extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "rule_key", nullable = false, unique = true)
    public String ruleKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false)
    public RepositoryRuleType ruleType;

    @Column(nullable = false)
    public String name;

    @Column(columnDefinition = "text")
    public String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public RepositoryRuleSeverity severity;

    @Column(nullable = false)
    public boolean enabled;

    @Column(name = "parameters_json", nullable = false, columnDefinition = "text")
    public String parametersJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public RepositoryRuleScope scope;

    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;
}
