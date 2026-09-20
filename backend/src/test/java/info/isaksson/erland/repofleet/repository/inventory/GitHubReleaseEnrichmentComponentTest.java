package info.isaksson.erland.repofleet.repository.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import info.isaksson.erland.repofleet.github.api.GitHubApiCallExecutor;
import info.isaksson.erland.repofleet.github.auth.GitHubInstallationToken;
import info.isaksson.erland.repofleet.github.auth.GitHubInstallationTokenService;
import info.isaksson.erland.repofleet.github.client.GitHubReleaseResponse;
import info.isaksson.erland.repofleet.github.client.GitHubRepositoryMetadataClient;
import info.isaksson.erland.repofleet.repository.api.ActivityStatus;
import info.isaksson.erland.repofleet.repository.api.AnalysisState;
import info.isaksson.erland.repofleet.repository.api.GitHubActionsStatus;
import info.isaksson.erland.repofleet.repository.api.LicensePresence;
import info.isaksson.erland.repofleet.repository.api.LicenseStatus;
import info.isaksson.erland.repofleet.repository.api.ReleaseStatus;
import info.isaksson.erland.repofleet.repository.api.RepositoryRefreshStatus;
import info.isaksson.erland.repofleet.repository.api.RepositorySummary;
import info.isaksson.erland.repofleet.repository.api.RepositoryVisibility;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GitHubReleaseEnrichmentComponentTest {

    private GitHubRepositoryMetadataClient client;
    private GitHubReleaseEnrichmentComponent component;

    @BeforeEach
    void setUp() {
        GitHubInstallationTokenService tokenService = mock(GitHubInstallationTokenService.class);
        when(tokenService.getToken()).thenReturn(
                new GitHubInstallationToken("token", Instant.parse("2026-09-20T10:00:00Z")));
        client = mock(GitHubRepositoryMetadataClient.class);
        component = new GitHubReleaseEnrichmentComponent(client, new GitHubApiCallExecutor(tokenService));
    }

    @Test
    void ignoresDraftsAndSelectsLatestPublishedRelease() {
        when(client.getReleases(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyInt(), eq(1)))
                .thenReturn(List.of(
                        release(1L, "draft", "v2.0.0", true, false, "2026-09-20T09:00:00Z"),
                        release(2L, "stable", "v1.0.0", false, false, "2026-09-19T09:00:00Z"),
                        release(3L, "beta", "v1.1.0-beta.1", false, true, "2026-09-20T08:00:00Z")));

        RepositoryMetadataResult<ReleaseStatus> result =
                component.enrich(repository(releaseUnknown()), false, true);

        assertTrue(result.complete());
        assertFalse(result.degraded());
        assertEquals(Boolean.TRUE, result.value().releasePresent());
        assertEquals("v1.1.0-beta.1", result.value().latestReleaseTag());
        assertEquals(Boolean.TRUE, result.value().latestReleasePrerelease());
    }

    @Test
    void scansAnotherPageWhenFirstPageOnlyContainsDrafts() {
        List<GitHubReleaseResponse> drafts = java.util.stream.IntStream.range(0, 100)
                .mapToObj(index -> release(index, "draft", "draft-" + index, true, false, "2026-09-20T08:00:00Z"))
                .toList();
        when(client.getReleases(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyInt(), eq(1)))
                .thenReturn(drafts);
        when(client.getReleases(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyInt(), eq(2)))
                .thenReturn(List.of(release(200L, "stable", "v1.0.0", false, false, "2026-09-18T08:00:00Z")));

        RepositoryMetadataResult<ReleaseStatus> result =
                component.enrich(repository(releaseUnknown()), false, true);

        assertTrue(result.complete());
        assertEquals("v1.0.0", result.value().latestReleaseTag());
    }

    @Test
    void retainsCachedReleaseAsCompleteDuringNormalRefreshFailure() {
        ReleaseStatus cached = new ReleaseStatus(
                AnalysisState.COMPLETE, true, "stable", "v1.0.0",
                Instant.parse("2026-09-18T08:00:00Z"), false);
        when(client.getReleases(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyInt(), anyInt()))
                .thenThrow(new IllegalStateException("releases unavailable"));

        RepositoryMetadataResult<ReleaseStatus> result =
                component.enrich(repository(cached), true, true);

        assertTrue(result.complete());
        assertTrue(result.degraded());
        assertEquals(cached, result.value());
    }

    @Test
    void volatileVerificationPreservesCacheButMarksFacetIncompleteOnFailure() {
        ReleaseStatus cached = new ReleaseStatus(
                AnalysisState.COMPLETE, true, "stable", "v1.0.0",
                Instant.parse("2026-09-18T08:00:00Z"), false);
        when(client.getReleases(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyInt(), anyInt()))
                .thenThrow(new IllegalStateException("releases unavailable"));

        RepositoryMetadataResult<ReleaseStatus> result =
                component.enrich(repository(cached), true, false);

        assertFalse(result.complete());
        assertTrue(result.degraded());
        assertEquals(cached, result.value());
    }

    @Test
    void failedReleaseAnalysisDoesNotClaimReleaseIsMissing() {
        when(client.getReleases(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyInt(), anyInt()))
                .thenThrow(new IllegalStateException("releases unavailable"));

        RepositoryMetadataResult<ReleaseStatus> result =
                component.enrich(repository(releaseUnknown()), false, true);

        assertFalse(result.complete());
        assertEquals(AnalysisState.FAILED, result.value().analysisState());
        assertNull(result.value().releasePresent());
    }

    private ReleaseStatus releaseUnknown() {
        return new ReleaseStatus(AnalysisState.NOT_ANALYZED, null, null, null, null, null);
    }

    private GitHubReleaseResponse release(
            long id, String name, String tag, boolean draft, boolean prerelease, String publishedAt) {
        Instant timestamp = Instant.parse(publishedAt);
        return new GitHubReleaseResponse(id, name, tag, draft, prerelease, timestamp, timestamp);
    }

    private RepositorySummary repository(ReleaseStatus release) {
        return new RepositorySummary(
                1L, "erland", "repo-fleet", "erland/repo-fleet",
                "https://github.com/erland/repo-fleet", RepositoryVisibility.PRIVATE,
                false, false, "main", List.of(), List.of(), null,
                new LicenseStatus(AnalysisState.NOT_ANALYZED, LicensePresence.UNKNOWN, null, null, null),
                new GitHubActionsStatus(AnalysisState.NOT_ANALYZED, null, null),
                release,
                new ActivityStatus(null, null),
                new RepositoryRefreshStatus(AnalysisState.NOT_ANALYZED, null));
    }
}
