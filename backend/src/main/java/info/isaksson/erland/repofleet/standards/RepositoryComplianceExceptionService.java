package info.isaksson.erland.repofleet.standards;

import info.isaksson.erland.repofleet.repository.persistence.RepositoryIdentityRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class RepositoryComplianceExceptionService {

    private final RepositoryIdentityRepository identities;
    private final Clock clock;

    @Inject
    public RepositoryComplianceExceptionService(
            RepositoryIdentityRepository identities) {
        this(identities, Clock.systemUTC());
    }

    RepositoryComplianceExceptionService(
            RepositoryIdentityRepository identities,
            Clock clock) {
        this.identities = identities;
        this.clock = clock;
    }

    @Transactional
    public RepositoryComplianceExceptionDefinition save(
            long githubRepositoryId,
            String ruleKey,
            String reason,
            Instant expiresAt,
            Instant now) {
        if (identities.findByGitHubRepositoryId(githubRepositoryId).isEmpty()) {
            throw new IllegalArgumentException("Unknown repository: " + githubRepositoryId);
        }
        if (RepositoryStandardRule.count("ruleKey", ruleKey) == 0) {
            throw new IllegalArgumentException("Unknown rule: " + ruleKey);
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("reason must not be blank");
        }

        RepositoryComplianceException entity = RepositoryComplianceException.find(
                        "githubRepositoryId = ?1 and ruleKey = ?2",
                        githubRepositoryId,
                        ruleKey)
                .firstResultOptional()
                .map(RepositoryComplianceException.class::cast)
                .orElseGet(RepositoryComplianceException::new);

        if (entity.id == null) {
            entity.githubRepositoryId = githubRepositoryId;
            entity.ruleKey = ruleKey;
            entity.createdAt = now;
        }
        entity.reason = reason;
        entity.expiresAt = expiresAt;
        entity.state = stateFor(expiresAt, now);
        entity.updatedAt = now;
        if (entity.id == null) {
            entity.persist();
        }
        return toDefinition(entity);
    }

    @Transactional
    public List<RepositoryComplianceExceptionDefinition> listForRepository(long repositoryId) {
        Instant now = clock.instant();
        return RepositoryComplianceException.list(
                        "githubRepositoryId = ?1 order by ruleKey",
                        repositoryId)
                .stream()
                .map(RepositoryComplianceException.class::cast)
                .map(entity -> refreshState(entity, now))
                .map(this::toDefinition)
                .toList();
    }

    @Transactional
    public boolean hasActiveException(long repositoryId, String ruleKey) {
        Instant now = clock.instant();
        RepositoryComplianceException entity = RepositoryComplianceException.find(
                        "githubRepositoryId = ?1 and ruleKey = ?2",
                        repositoryId,
                        ruleKey)
                .firstResultOptional()
                .map(RepositoryComplianceException.class::cast)
                .orElse(null);
        if (entity == null) {
            return false;
        }
        refreshState(entity, now);
        return entity.state == RepositoryComplianceExceptionState.ACTIVE;
    }

    @Transactional
    public Set<String> activeKeys() {
        Instant now = clock.instant();
        return RepositoryComplianceException.listAll().stream()
                .map(RepositoryComplianceException.class::cast)
                .map(entity -> refreshState(entity, now))
                .filter(entity -> entity.state == RepositoryComplianceExceptionState.ACTIVE)
                .map(entity -> key(entity.githubRepositoryId, entity.ruleKey))
                .collect(Collectors.toSet());
    }

    public String key(long repositoryId, String ruleKey) {
        return repositoryId + ":" + ruleKey;
    }

    private RepositoryComplianceException refreshState(
            RepositoryComplianceException entity,
            Instant now) {
        RepositoryComplianceExceptionState expected = stateFor(entity.expiresAt, now);
        if (entity.state != expected) {
            entity.state = expected;
            entity.updatedAt = now;
        }
        return entity;
    }

    private RepositoryComplianceExceptionState stateFor(
            Instant expiresAt,
            Instant now) {
        return expiresAt != null && !expiresAt.isAfter(now)
                ? RepositoryComplianceExceptionState.EXPIRED
                : RepositoryComplianceExceptionState.ACTIVE;
    }

    private RepositoryComplianceExceptionDefinition toDefinition(
            RepositoryComplianceException entity) {
        return new RepositoryComplianceExceptionDefinition(
                entity.githubRepositoryId,
                entity.ruleKey,
                entity.reason,
                entity.expiresAt,
                entity.state,
                entity.createdAt,
                entity.updatedAt);
    }
}
