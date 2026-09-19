package info.isaksson.erland.repofleet.repository.persistence;

import info.isaksson.erland.repofleet.github.conditional.GitHubConditionalRequestStateService;
import info.isaksson.erland.repofleet.repository.api.RepositorySummary;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ApplicationScoped
public class RepositoryInventoryPersistenceService {

    private final RepositoryIdentityRepository repository;
    private final RepositoryChangeFingerprintService changeFingerprintService;
    private final GitHubConditionalRequestStateService conditionalStateService;

    @Inject
    public RepositoryInventoryPersistenceService(
            RepositoryIdentityRepository repository,
            RepositoryChangeFingerprintService changeFingerprintService,
            GitHubConditionalRequestStateService conditionalStateService) {
        this.repository = repository;
        this.changeFingerprintService = changeFingerprintService;
        this.conditionalStateService = conditionalStateService;
    }

    @Transactional
    public void invalidateConditionalState(List<RepositorySummary> repositories, Instant invalidatedAt) {
        for (RepositorySummary summary : repositories) {
            conditionalStateService.invalidateAll(summary.id(), invalidatedAt);
        }
    }

    @Transactional
    public void synchronize(List<RepositorySummary> discovered, Instant seenAt) {
        Set<Long> seenRepositoryIds = new HashSet<>();

        for (RepositorySummary summary : discovered) {
            seenRepositoryIds.add(summary.id());

            var existing = repository.findByGitHubRepositoryId(summary.id());
            RepositoryChangeClassification classification =
                    changeFingerprintService.classify(existing.orElse(null), summary);
            RepositoryIdentity stored;
            if (existing.isPresent()) {
                stored = repository.update(
                        summary.id(),
                        summary.owner(),
                        summary.name(),
                        summary.fullName(),
                        summary.visibility(),
                        summary.archived(),
                        summary.fork(),
                        summary.defaultBranch(),
                        summary.activity() == null ? null : summary.activity().updatedAt(),
                        summary.activity() == null ? null : summary.activity().pushedAt(),
                        seenAt,
                        true);
            } else {
                stored = repository.insert(
                        summary.id(),
                        summary.owner(),
                        summary.name(),
                        summary.fullName(),
                        summary.visibility(),
                        summary.archived(),
                        summary.fork(),
                        summary.defaultBranch(),
                        summary.activity() == null ? null : summary.activity().updatedAt(),
                        summary.activity() == null ? null : summary.activity().pushedAt(),
                        seenAt);
            }
            stored.changeClassification = classification.name();
            stored.changeDetectedAt = seenAt;
            if (classification == RepositoryChangeClassification.LIKELY_CHANGED) {
                conditionalStateService.invalidateAll(summary.id(), seenAt);
            }
        }

        repository.markMissingRepositoriesInactive(seenRepositoryIds);
    }
}
