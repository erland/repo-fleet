package info.isaksson.erland.repofleet.repository.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import info.isaksson.erland.repofleet.repository.api.RepositorySummary;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class InMemoryRepositoryInventoryServiceTest {

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-08-14T06:00:00Z"), ZoneOffset.UTC);

    @Test
    void repeatedReadsUseCurrentInventoryWithoutRediscovery() {
        AtomicInteger calls = new AtomicInteger();
        List<RepositorySummary> expected = List.of();

        GitHubRepositoryDiscoveryService discovery = () -> {
            calls.incrementAndGet();
            return expected;
        };

        var service = new InMemoryRepositoryInventoryService(discovery, repository -> repository, CLOCK);
        service.refresh();

        assertSame(service.listRepositories(), service.listRepositories());
        assertEquals(1, calls.get());
        assertEquals(InventoryRefreshState.COMPLETED, service.getStatus().state());
    }

    @Test
    void explicitRefreshReplacesInventory() {
        AtomicInteger calls = new AtomicInteger();
        List<List<RepositorySummary>> responses = new ArrayList<>();
        responses.add(List.of());
        responses.add(List.of());

        GitHubRepositoryDiscoveryService discovery =
                () -> responses.get(Math.min(calls.getAndIncrement(), responses.size() - 1));

        var service = new InMemoryRepositoryInventoryService(discovery, repository -> repository, CLOCK);
        service.refresh();
        service.refresh();

        assertEquals(2, calls.get());
        assertEquals(InventoryRefreshState.COMPLETED, service.getStatus().state());
    }

    @Test
    void failedRefreshRetainsPreviousInventory() {
        AtomicInteger calls = new AtomicInteger();
        GitHubRepositoryDiscoveryService discovery = () -> {
            if (calls.getAndIncrement() == 0) {
                return List.of();
            }
            throw new IllegalStateException("GitHub unavailable");
        };

        var service = new InMemoryRepositoryInventoryService(discovery, repository -> repository, CLOCK);
        service.refresh();
        List<RepositorySummary> previous = service.listRepositories();

        service.refresh();

        assertSame(previous, service.listRepositories());
        assertEquals(InventoryRefreshState.FAILED, service.getStatus().state());
        assertEquals("GitHub unavailable", service.getStatus().errorMessage());
        assertEquals(Instant.parse("2026-08-14T06:00:00Z"), service.getStatus().lastSuccessfulRefreshAt());
    }

    @Test
    void individualEnrichmentFailureDoesNotFailWholeRefresh() {
        GitHubRepositoryDiscoveryService discovery = () -> List.of(repository(1L, "one"), repository(2L, "two"));
        RepositoryEnrichmentService enrichment = repository -> {
            if (repository.id() == 2L) {
                throw new IllegalStateException("metadata unavailable");
            }
            return repository;
        };

        var service = new InMemoryRepositoryInventoryService(discovery, enrichment, CLOCK);
        service.refresh();

        assertEquals(InventoryRefreshState.COMPLETED, service.getStatus().state());
        assertEquals(2, service.listRepositories().size());
        assertEquals(
                info.isaksson.erland.repofleet.repository.api.AnalysisState.FAILED,
                service.listRepositories().get(1).refreshStatus().state());
    }

    private RepositorySummary repository(long id, String name) {
        return new RepositorySummary(
                id,
                "erland",
                name,
                "erland/" + name,
                "https://github.com/erland/" + name,
                info.isaksson.erland.repofleet.repository.api.RepositoryVisibility.PRIVATE,
                false,
                false,
                "main",
                List.of(),
                List.of(),
                null,
                new info.isaksson.erland.repofleet.repository.api.LicenseStatus(
                        info.isaksson.erland.repofleet.repository.api.AnalysisState.NOT_ANALYZED,
                        info.isaksson.erland.repofleet.repository.api.LicensePresence.UNKNOWN,
                        null, null, null),
                new info.isaksson.erland.repofleet.repository.api.GitHubActionsStatus(
                        info.isaksson.erland.repofleet.repository.api.AnalysisState.NOT_ANALYZED,
                        null, null),
                new info.isaksson.erland.repofleet.repository.api.ReleaseStatus(
                        info.isaksson.erland.repofleet.repository.api.AnalysisState.NOT_ANALYZED,
                        null, null, null, null, null),
                new info.isaksson.erland.repofleet.repository.api.ActivityStatus(null, null),
                new info.isaksson.erland.repofleet.repository.api.RepositoryRefreshStatus(
                        info.isaksson.erland.repofleet.repository.api.AnalysisState.NOT_ANALYZED,
                        "pending"));
    }

}
