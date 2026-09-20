package info.isaksson.erland.repofleet.repository.inventory;

import info.isaksson.erland.repofleet.github.api.GitHubApiCallExecutor;
import info.isaksson.erland.repofleet.github.auth.GitHubInstallationTokenService;
import info.isaksson.erland.repofleet.github.client.GitHubReleaseResponse;
import info.isaksson.erland.repofleet.github.client.GitHubRepositoryMetadataClient;
import info.isaksson.erland.repofleet.github.conditional.GitHubConditionalRequestExecutor;
import info.isaksson.erland.repofleet.repository.api.AnalysisState;
import info.isaksson.erland.repofleet.repository.api.ReleaseStatus;
import info.isaksson.erland.repofleet.repository.api.RepositorySummary;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.GenericType;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
class GitHubReleaseEnrichmentComponent {

    private final GitHubRepositoryMetadataClient client;
    private final GitHubApiCallExecutor apiCalls;
    private final GitHubConditionalRequestExecutor conditionalRequests;

    @Inject
    GitHubReleaseEnrichmentComponent(
            @RestClient GitHubRepositoryMetadataClient client,
            GitHubApiCallExecutor apiCalls,
            GitHubConditionalRequestExecutor conditionalRequests) {
        this.client = client;
        this.apiCalls = apiCalls;
        this.conditionalRequests = conditionalRequests;
    }

    GitHubReleaseEnrichmentComponent(
            GitHubRepositoryMetadataClient client,
            GitHubApiCallExecutor apiCalls) {
        this(client, apiCalls, null);
    }

    RepositoryMetadataResult<ReleaseStatus> enrich(
            RepositorySummary repository,
            boolean cachedComplete,
            boolean allowFailureFallback) {
        ReleaseStatus cached = repository.release();
        try {
            ReleaseStatus value;
            if (conditionalRequests == null) {
                value = toStatus(findLatestPublishedRelease(repository));
            } else {
                var result = conditionalRequests.execute(
                        repository.id(),
                        "releases",
                        "releases for " + repository.fullName() + " page 1",
                        Instant.now(),
                        (authorization, etag) -> client.getReleasesConditional(
                                repository.owner(),
                                repository.name(),
                                authorization,
                                GitHubInstallationTokenService.ACCEPT,
                                GitHubInstallationTokenService.API_VERSION,
                                etag,
                                100,
                                1),
                        response -> response.readEntity(new GenericType<List<GitHubReleaseResponse>>() {}),
                        () -> null);
                value = result.reusedCached()
                        ? cachedComplete ? cached : toStatus(findLatestPublishedRelease(repository))
                        : toStatus(findLatestPublishedRelease(repository, result.value()));
            }
            return RepositoryMetadataResult.success(value);
        } catch (RuntimeException exception) {
            String error = "release: " + safeMessage(exception);
            if (allowFailureFallback && cachedComplete) {
                return RepositoryMetadataResult.degraded(cached, true, error);
            }
            return RepositoryMetadataResult.degraded(
                    new ReleaseStatus(AnalysisState.FAILED, null, null, null, null, null),
                    false,
                    error);
        }
    }

    private GitHubReleaseResponse findLatestPublishedRelease(
            RepositorySummary repository,
            List<GitHubReleaseResponse> firstPage) {
        List<GitHubReleaseResponse> releases = firstPage == null ? List.of() : firstPage;
        GitHubReleaseResponse latest = latestPublished(releases);
        if (latest != null || releases.size() < 100) {
            return latest;
        }
        return findLatestPublishedReleaseFromPage(repository, 2);
    }

    private GitHubReleaseResponse latestPublished(List<GitHubReleaseResponse> releases) {
        return releases.stream()
                .filter(item -> item != null && !item.draft())
                .max(Comparator.comparing(
                        this::releaseTimestamp,
                        Comparator.nullsFirst(Comparator.naturalOrder())))
                .orElse(null);
    }

    private GitHubReleaseResponse findLatestPublishedReleaseFromPage(
            RepositorySummary repository,
            int startPage) {
        int page = startPage;
        while (true) {
            int requestedPage = page;
            List<GitHubReleaseResponse> response = apiCalls.execute(
                    "releases for " + repository.fullName() + " page " + requestedPage,
                    authorization -> client.getReleases(
                            repository.owner(),
                            repository.name(),
                            authorization,
                            GitHubInstallationTokenService.ACCEPT,
                            GitHubInstallationTokenService.API_VERSION,
                            100,
                            requestedPage));
            List<GitHubReleaseResponse> releases = response == null ? List.of() : response;
            GitHubReleaseResponse latest = latestPublished(releases);
            if (latest != null || releases.size() < 100) {
                return latest;
            }
            page++;
        }
    }

    private GitHubReleaseResponse findLatestPublishedRelease(RepositorySummary repository) {
        List<GitHubReleaseResponse> response = apiCalls.execute(
                "releases for " + repository.fullName() + " page 1",
                authorization -> client.getReleases(
                        repository.owner(),
                        repository.name(),
                        authorization,
                        GitHubInstallationTokenService.ACCEPT,
                        GitHubInstallationTokenService.API_VERSION,
                        100,
                        1));
        return findLatestPublishedRelease(repository, response);
    }

    private Instant releaseTimestamp(GitHubReleaseResponse release) {
        return release.publishedAt() != null ? release.publishedAt() : release.createdAt();
    }

    private ReleaseStatus toStatus(GitHubReleaseResponse latest) {
        return latest == null
                ? new ReleaseStatus(AnalysisState.COMPLETE, false, null, null, null, null)
                : new ReleaseStatus(
                        AnalysisState.COMPLETE,
                        true,
                        latest.name(),
                        latest.tagName(),
                        releaseTimestamp(latest),
                        latest.prerelease());
    }

    private String safeMessage(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank()
                ? exception.getClass().getSimpleName()
                : message;
    }
}
