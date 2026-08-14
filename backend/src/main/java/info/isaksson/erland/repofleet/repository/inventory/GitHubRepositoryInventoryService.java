package info.isaksson.erland.repofleet.repository.inventory;

import info.isaksson.erland.repofleet.github.auth.GitHubInstallationTokenService;
import info.isaksson.erland.repofleet.github.client.GitHubAppClient;
import info.isaksson.erland.repofleet.github.client.GitHubRepositoryResponse;
import info.isaksson.erland.repofleet.github.client.InstallationRepositoriesResponse;
import info.isaksson.erland.repofleet.repository.api.ActivityStatus;
import info.isaksson.erland.repofleet.repository.api.AnalysisState;
import info.isaksson.erland.repofleet.repository.api.GitHubActionsStatus;
import info.isaksson.erland.repofleet.repository.api.LicensePresence;
import info.isaksson.erland.repofleet.repository.api.LicenseStatus;
import info.isaksson.erland.repofleet.repository.api.ReleaseStatus;
import info.isaksson.erland.repofleet.repository.api.RepositoryRefreshStatus;
import info.isaksson.erland.repofleet.repository.api.RepositorySummary;
import info.isaksson.erland.repofleet.repository.api.RepositoryVisibility;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@ApplicationScoped
public class GitHubRepositoryInventoryService implements GitHubRepositoryDiscoveryService {

    static final int PAGE_SIZE = 100;

    private final GitHubInstallationTokenService tokenService;
    private final GitHubAppClient client;

    @Inject
    public GitHubRepositoryInventoryService(
        GitHubInstallationTokenService tokenService,
        @RestClient GitHubAppClient client
    ) {
        this.tokenService = tokenService;
        this.client = client;
    }

    @Override
    public List<RepositorySummary> discoverRepositories() {
        String token = tokenService.getToken().value();
        List<RepositorySummary> repositories = new ArrayList<>();
        int page = 1;
        int totalCount = Integer.MAX_VALUE;

        while (repositories.size() < totalCount) {
            InstallationRepositoriesResponse response = client.listInstallationRepositories(
                "Bearer " + token,
                GitHubInstallationTokenService.ACCEPT,
                GitHubInstallationTokenService.API_VERSION,
                PAGE_SIZE,
                page
            );
            if (response == null) {
                throw new IllegalStateException("GitHub returned an empty repository discovery response.");
            }
            totalCount = Math.max(0, response.totalCount());
            List<GitHubRepositoryResponse> currentPage = response.repositories();
            if (currentPage.isEmpty()) {
                break;
            }
            currentPage.stream().map(this::mapRepository).forEach(repositories::add);
            if (currentPage.size() < PAGE_SIZE) {
                break;
            }
            page++;
        }

        return repositories.stream()
            .sorted(Comparator.comparing(RepositorySummary::fullName, String.CASE_INSENSITIVE_ORDER))
            .toList();
    }

    RepositorySummary mapRepository(GitHubRepositoryResponse repository) {
        String owner = repository.owner() == null ? null : repository.owner().login();
        return new RepositorySummary(
            repository.id(),
            owner,
            repository.name(),
            repository.fullName(),
            repository.htmlUrl(),
            mapVisibility(repository),
            repository.archived(),
            repository.fork(),
            repository.defaultBranch(),
            List.of(),
            List.of(),
            null,
            new LicenseStatus(AnalysisState.NOT_ANALYZED, LicensePresence.UNKNOWN, null, null, null),
            new GitHubActionsStatus(AnalysisState.NOT_ANALYZED, null, null),
            new ReleaseStatus(AnalysisState.NOT_ANALYZED, null, null, null, null, null),
            new ActivityStatus(repository.pushedAt(), repository.updatedAt()),
            new RepositoryRefreshStatus(AnalysisState.NOT_ANALYZED, "Repository discovered; maintenance enrichment pending.")
        );
    }

    private RepositoryVisibility mapVisibility(GitHubRepositoryResponse repository) {
        String visibility = repository.visibility();
        if (visibility != null) {
            try {
                return RepositoryVisibility.valueOf(visibility.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                // Fall back to the private flag for forward compatibility with unknown values.
            }
        }
        return repository.privateRepository() ? RepositoryVisibility.PRIVATE : RepositoryVisibility.PUBLIC;
    }
}
