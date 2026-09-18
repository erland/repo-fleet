package info.isaksson.erland.repofleet.repository.refresh;

import info.isaksson.erland.repofleet.repository.api.RepositorySummary;
import info.isaksson.erland.repofleet.repository.persistence.RepositoryChangeClassification;
import info.isaksson.erland.repofleet.repository.persistence.RepositoryEnrichmentSnapshotRepository;
import info.isaksson.erland.repofleet.repository.persistence.RepositoryEnrichmentSnapshotService;
import info.isaksson.erland.repofleet.repository.persistence.RepositoryIdentityRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class RepositoryRefreshPlanner {

    private final RepositoryIdentityRepository identityRepository;
    private final RepositoryEnrichmentSnapshotRepository snapshotRepository;
    private final RepositoryEnrichmentSnapshotService snapshotService;

    @Inject
    public RepositoryRefreshPlanner(
            RepositoryIdentityRepository identityRepository,
            RepositoryEnrichmentSnapshotRepository snapshotRepository,
            RepositoryEnrichmentSnapshotService snapshotService) {
        this.identityRepository = identityRepository;
        this.snapshotRepository = snapshotRepository;
        this.snapshotService = snapshotService;
    }

    @Transactional
    public RepositoryRefreshPlan plan(List<RepositorySummary> discovered) {
        List<RepositoryRefreshPlanItem> items = new ArrayList<>();
        int reused = 0;
        int newRepositories = 0;
        int changed = 0;
        int scheduled = 0;

        for (RepositorySummary summary : discovered) {
            var identity = identityRepository.findByGitHubRepositoryId(summary.id()).orElseThrow();
            RepositoryChangeClassification classification =
                    identity.changeClassification == null
                            ? RepositoryChangeClassification.LIKELY_CHANGED
                            : RepositoryChangeClassification.valueOf(identity.changeClassification);

            RepositoryRefreshAction action;
            RepositorySummary cached = null;

            if (classification == RepositoryChangeClassification.NEW) {
                action = RepositoryRefreshAction.FULL_ENRICHMENT_NEW;
                newRepositories++;
                scheduled++;
            } else if (classification == RepositoryChangeClassification.LIKELY_CHANGED) {
                action = RepositoryRefreshAction.FULL_ENRICHMENT;
                changed++;
                scheduled++;
            } else if (snapshotRepository.findByGitHubRepositoryId(summary.id()).isPresent()) {
                action = RepositoryRefreshAction.REUSE_CACHED;
                cached = snapshotService.reconstruct(identity);
                reused++;
            } else {
                action = RepositoryRefreshAction.FULL_ENRICHMENT;
                scheduled++;
            }

            items.add(new RepositoryRefreshPlanItem(summary, action, cached));
        }

        return new RepositoryRefreshPlan(
                List.copyOf(items),
                reused,
                newRepositories,
                changed,
                scheduled);
    }
}
