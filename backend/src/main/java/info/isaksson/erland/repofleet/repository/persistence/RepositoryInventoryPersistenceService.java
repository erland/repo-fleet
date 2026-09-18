package info.isaksson.erland.repofleet.repository.persistence;

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

    @Inject
    public RepositoryInventoryPersistenceService(RepositoryIdentityRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void synchronize(List<RepositorySummary> discovered, Instant seenAt) {
        Set<Long> seenRepositoryIds = new HashSet<>();

        for (RepositorySummary summary : discovered) {
            seenRepositoryIds.add(summary.id());

            var existing = repository.findByGitHubRepositoryId(summary.id());
            if (existing.isPresent()) {
                repository.update(
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
                repository.insert(
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
        }

        repository.markMissingRepositoriesInactive(seenRepositoryIds);
    }
}
