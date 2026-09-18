package info.isaksson.erland.repofleet.repository.persistence;

import info.isaksson.erland.repofleet.repository.api.RepositoryVisibility;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(
        name = "repository_identity",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_repository_identity_github_repository_id",
                columnNames = "github_repository_id"))
public class RepositoryIdentity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "github_repository_id", nullable = false, updatable = false)
    public long githubRepositoryId;

    @Column(name = "owner_login", nullable = false)
    public String ownerLogin;

    @Column(nullable = false)
    public String name;

    @Column(name = "full_name", nullable = false)
    public String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public RepositoryVisibility visibility;

    @Column(nullable = false)
    public boolean archived;

    @Column(nullable = false)
    public boolean fork;

    @Column(name = "default_branch")
    public String defaultBranch;

    @Column(name = "github_updated_at")
    public Instant githubUpdatedAt;

    @Column(name = "github_pushed_at")
    public Instant githubPushedAt;

    @Column(name = "first_seen_at", nullable = false, updatable = false)
    public Instant firstSeenAt;

    @Column(name = "last_seen_at", nullable = false)
    public Instant lastSeenAt;

    @Column(nullable = false)
    public boolean active;
}
