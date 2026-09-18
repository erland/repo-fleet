package info.isaksson.erland.repofleet.repository.persistence;

import info.isaksson.erland.repofleet.repository.api.RepositorySummary;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.Comparator;
import java.util.List;

@ApplicationScoped
public class CachedRepositoryInventoryService {

    private final RepositoryIdentityRepository repository;
    private final RepositoryEnrichmentSnapshotService snapshotService;

    @Inject
    public CachedRepositoryInventoryService(
            RepositoryIdentityRepository repository,
            RepositoryEnrichmentSnapshotService snapshotService) {
        this.repository = repository;
        this.snapshotService = snapshotService;
    }

    @Transactional
    public List<RepositorySummary> loadActiveRepositories() {
        return repository.list("active", true).stream()
                .map(snapshotService::reconstruct)
                .sorted(Comparator.comparing(RepositorySummary::fullName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

}
