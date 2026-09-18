package info.isaksson.erland.repofleet.repository.persistence;

import info.isaksson.erland.repofleet.repository.api.ActivityStatus;
import info.isaksson.erland.repofleet.repository.api.AnalysisState;
import info.isaksson.erland.repofleet.repository.api.GitHubActionsStatus;
import info.isaksson.erland.repofleet.repository.api.LicensePresence;
import info.isaksson.erland.repofleet.repository.api.LicenseStatus;
import info.isaksson.erland.repofleet.repository.api.ReleaseStatus;
import info.isaksson.erland.repofleet.repository.api.RepositoryRefreshStatus;
import info.isaksson.erland.repofleet.repository.api.RepositorySummary;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.Comparator;
import java.util.List;

@ApplicationScoped
public class CachedRepositoryInventoryService {

    private final RepositoryIdentityRepository repository;

    @Inject
    public CachedRepositoryInventoryService(RepositoryIdentityRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public List<RepositorySummary> loadActiveRepositories() {
        return repository.list("active", true).stream()
                .map(this::toSummary)
                .sorted(Comparator.comparing(RepositorySummary::fullName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private RepositorySummary toSummary(RepositoryIdentity entity) {
        return new RepositorySummary(
                entity.githubRepositoryId,
                entity.ownerLogin,
                entity.name,
                entity.fullName,
                "https://github.com/" + entity.fullName,
                entity.visibility,
                entity.archived,
                entity.fork,
                entity.defaultBranch,
                List.of(),
                List.of(),
                null,
                new LicenseStatus(
                        AnalysisState.NOT_ANALYZED,
                        LicensePresence.UNKNOWN,
                        null,
                        null,
                        null),
                new GitHubActionsStatus(AnalysisState.NOT_ANALYZED, null, null),
                new ReleaseStatus(AnalysisState.NOT_ANALYZED, null, null, null, null, null),
                new ActivityStatus(entity.githubPushedAt, entity.githubUpdatedAt),
                new RepositoryRefreshStatus(
                        AnalysisState.NOT_ANALYZED,
                        "Loaded from persisted repository cache; enrichment refresh pending."));
    }
}
