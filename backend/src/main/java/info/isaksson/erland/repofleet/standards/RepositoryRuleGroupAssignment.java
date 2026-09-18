package info.isaksson.erland.repofleet.standards;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "repository_rule_group_assignment")
public class RepositoryRuleGroupAssignment extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "rule_key", nullable = false)
    public String ruleKey;

    @Column(name = "group_key", nullable = false)
    public String groupKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt;
}
