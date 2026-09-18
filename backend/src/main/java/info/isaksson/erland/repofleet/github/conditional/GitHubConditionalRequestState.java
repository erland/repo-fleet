package info.isaksson.erland.repofleet.github.conditional;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "github_conditional_request_state")
public class GitHubConditionalRequestState extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "github_repository_id", nullable = false)
    public long githubRepositoryId;

    @Column(name = "resource_category", nullable = false)
    public String resourceCategory;

    @Column(name = "etag")
    public String etag;

    @Column(name = "last_successful_fetch_at")
    public Instant lastSuccessfulFetchAt;

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;
}
