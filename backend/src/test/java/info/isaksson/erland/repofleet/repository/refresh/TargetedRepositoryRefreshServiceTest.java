package info.isaksson.erland.repofleet.repository.refresh;

import static org.mockito.Mockito.*;

import info.isaksson.erland.repofleet.repository.api.*;
import info.isaksson.erland.repofleet.repository.inventory.InMemoryRepositoryInventoryService;
import info.isaksson.erland.repofleet.repository.inventory.RepositoryEnrichmentService;
import info.isaksson.erland.repofleet.repository.persistence.RepositoryEnrichmentSnapshotService;
import info.isaksson.erland.repofleet.repository.persistence.RepositoryIdentity;
import info.isaksson.erland.repofleet.repository.persistence.RepositoryIdentityRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TargetedRepositoryRefreshServiceTest {

    @Test
    void refreshesExactlyOneRepositoryFromPersistedIdentityAndSnapshot() {
        RepositoryIdentityRepository identities = mock(RepositoryIdentityRepository.class);
        RepositoryEnrichmentSnapshotService snapshots = mock(RepositoryEnrichmentSnapshotService.class);
        RepositoryEnrichmentService enrichment = mock(RepositoryEnrichmentService.class);
        InMemoryRepositoryInventoryService inventory = mock(InMemoryRepositoryInventoryService.class);

        RepositoryIdentity identity = new RepositoryIdentity();
        identity.githubRepositoryId = 701L;
        identity.ownerLogin = "erland";
        identity.name = "target";
        identity.fullName = "erland/target";
        identity.visibility = RepositoryVisibility.PRIVATE;
        identity.defaultBranch = "main";
        identity.active = true;

        RepositorySummary cached = repository(701L, "cached");
        RepositorySummary enriched = repository(701L, "enriched");
        RepositorySummary refreshed = repository(701L, "refreshed");
        Instant now = Instant.parse("2026-09-18T15:30:00Z");

        when(identities.findByGitHubRepositoryId(701L)).thenReturn(Optional.of(identity));
        when(snapshots.reconstruct(identity)).thenReturn(cached, refreshed);
        when(enrichment.enrich(cached)).thenReturn(enriched);

        TargetedRepositoryRefreshService service =
                new TargetedRepositoryRefreshService(
                        identities,
                        snapshots,
                        enrichment,
                        inventory);

        service.refresh(701L, now);

        verify(identities, times(1)).findByGitHubRepositoryId(701L);
        verify(snapshots, times(2)).reconstruct(identity);
        verify(enrichment, times(1)).enrich(cached);
        verify(snapshots, times(1)).persistProgressiveResult(enriched, now);
        verify(inventory, times(1)).replaceRepository(refreshed);
        verifyNoMoreInteractions(enrichment);
    }

    private RepositorySummary repository(long id, String message) {
        Instant now = Instant.parse("2026-09-18T15:00:00Z");
        return new RepositorySummary(
                id,
                "erland",
                "target",
                "erland/target",
                "https://github.com/erland/target",
                RepositoryVisibility.PRIVATE,
                false,
                false,
                "main",
                List.of(),
                List.of("Java"),
                "Java",
                new LicenseStatus(
                        AnalysisState.COMPLETE,
                        LicensePresence.PRESENT,
                        true,
                        "mit",
                        "MIT"),
                new GitHubActionsStatus(AnalysisState.COMPLETE, true, 1),
                new ReleaseStatus(AnalysisState.COMPLETE, false, null, null, null, null),
                new ActivityStatus(now, now),
                new RepositoryRefreshStatus(AnalysisState.COMPLETE, message));
    }
}
