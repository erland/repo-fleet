package info.isaksson.erland.repofleet.repository.persistence;

import info.isaksson.erland.repofleet.repository.api.RepositoryVisibility;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.Optional;

@ApplicationScoped
public class RepositoryIdentityRepository implements PanacheRepository<RepositoryIdentity> {

    public Optional<RepositoryIdentity> findByGitHubRepositoryId(long githubRepositoryId) {
        return find("githubRepositoryId", githubRepositoryId).firstResultOptional();
    }

    @Transactional
    public RepositoryIdentity insert(
            long githubRepositoryId,
            String ownerLogin,
            String name,
            String fullName,
            RepositoryVisibility visibility,
            boolean archived,
            boolean fork,
            String defaultBranch,
            Instant githubUpdatedAt,
            Instant githubPushedAt,
            Instant seenAt) {
        RepositoryIdentity entity = new RepositoryIdentity();
        entity.githubRepositoryId = githubRepositoryId;
        entity.ownerLogin = ownerLogin;
        entity.name = name;
        entity.fullName = fullName;
        entity.visibility = visibility;
        entity.archived = archived;
        entity.fork = fork;
        entity.defaultBranch = defaultBranch;
        entity.githubUpdatedAt = githubUpdatedAt;
        entity.githubPushedAt = githubPushedAt;
        entity.firstSeenAt = seenAt;
        entity.lastSeenAt = seenAt;
        entity.active = true;
        persist(entity);
        return entity;
    }

    @Transactional
    public RepositoryIdentity update(
            long githubRepositoryId,
            String ownerLogin,
            String name,
            String fullName,
            RepositoryVisibility visibility,
            boolean archived,
            boolean fork,
            String defaultBranch,
            Instant githubUpdatedAt,
            Instant githubPushedAt,
            Instant seenAt,
            boolean active) {
        RepositoryIdentity entity = findByGitHubRepositoryId(githubRepositoryId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown GitHub repository ID: " + githubRepositoryId));
        entity.ownerLogin = ownerLogin;
        entity.name = name;
        entity.fullName = fullName;
        entity.visibility = visibility;
        entity.archived = archived;
        entity.fork = fork;
        entity.defaultBranch = defaultBranch;
        entity.githubUpdatedAt = githubUpdatedAt;
        entity.githubPushedAt = githubPushedAt;
        entity.lastSeenAt = seenAt;
        entity.active = active;
        return entity;
    }
}
