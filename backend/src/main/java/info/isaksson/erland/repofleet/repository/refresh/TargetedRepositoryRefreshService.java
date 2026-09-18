package info.isaksson.erland.repofleet.repository.refresh;

import info.isaksson.erland.repofleet.repository.inventory.InMemoryRepositoryInventoryService;
import info.isaksson.erland.repofleet.repository.inventory.RepositoryEnrichmentService;
import info.isaksson.erland.repofleet.repository.persistence.RepositoryEnrichmentSnapshotService;
import info.isaksson.erland.repofleet.repository.persistence.RepositoryIdentityRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Instant;

@ApplicationScoped
public class TargetedRepositoryRefreshService {

    private final RepositoryIdentityRepository identities;
    private final RepositoryEnrichmentSnapshotService snapshots;
    private final RepositoryEnrichmentService enrichment;
    private final InMemoryRepositoryInventoryService inventory;

    @Inject
    public TargetedRepositoryRefreshService(
            RepositoryIdentityRepository identities,
            RepositoryEnrichmentSnapshotService snapshots,
            RepositoryEnrichmentService enrichment,
            InMemoryRepositoryInventoryService inventory) {
        this.identities = identities;
        this.snapshots = snapshots;
        this.enrichment = enrichment;
        this.inventory = inventory;
    }

    @Transactional
    public void refresh(long repositoryId, Instant now) {
        var identity = identities.findByGitHubRepositoryId(repositoryId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown repository: " + repositoryId));
        if (!identity.active) {
            throw new IllegalStateException(
                    "Cannot refresh inactive repository: " + repositoryId);
        }

        var cached = snapshots.reconstruct(identity);
        var enriched = enrichment.enrich(cached);
        snapshots.persistProgressiveResult(enriched, now);

        var refreshed = snapshots.reconstruct(identity);
        inventory.replaceRepository(refreshed);
    }
}
