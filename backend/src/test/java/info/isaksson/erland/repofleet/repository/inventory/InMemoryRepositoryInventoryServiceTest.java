package info.isaksson.erland.repofleet.repository.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import info.isaksson.erland.repofleet.repository.api.AnalysisState;
import info.isaksson.erland.repofleet.repository.api.RepositorySummary;
import info.isaksson.erland.repofleet.repository.persistence.CachedRepositoryInventoryService;
import info.isaksson.erland.repofleet.repository.persistence.RepositoryEnrichmentSnapshotService;
import info.isaksson.erland.repofleet.repository.refresh.RepositoryRefreshAction;
import info.isaksson.erland.repofleet.repository.refresh.RepositoryRefreshPlan;
import info.isaksson.erland.repofleet.repository.refresh.RepositoryRefreshPlanItem;
import info.isaksson.erland.repofleet.repository.refresh.RepositoryRefreshPlanner;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class InMemoryRepositoryInventoryServiceTest {

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-08-14T06:00:00Z"), ZoneOffset.UTC);

    @Test
    void reusesCachedRepositoryWithoutCallingEnrichment() {
        RepositorySummary discovered = repository(1L, "one");
        RepositorySummary cached = complete(discovered);
        AtomicInteger enrichmentCalls = new AtomicInteger();

        RepositoryRefreshPlanner planner = org.mockito.Mockito.mock(RepositoryRefreshPlanner.class);
        org.mockito.Mockito.when(planner.plan(List.of(discovered)))
                .thenReturn(new RepositoryRefreshPlan(
                        List.of(new RepositoryRefreshPlanItem(
                                discovered,
                                RepositoryRefreshAction.REUSE_CACHED,
                                cached)),
                        1,
                        0,
                        0,
                        0));

        var service = new InMemoryRepositoryInventoryService(
                () -> List.of(discovered),
                repository -> {
                    enrichmentCalls.incrementAndGet();
                    return complete(repository);
                },
                CLOCK,
                null,
                null,
                null,
                null,
                null,
                planner);

        service.refresh();

        assertEquals(0, enrichmentCalls.get());
        assertEquals(1, service.getStatus().reusedCount());
        assertEquals(0, service.getStatus().scheduledCount());
        assertEquals(InventoryRefreshState.COMPLETED, service.getStatus().state());
        assertEquals(AnalysisState.COMPLETE, service.listRepositories().getFirst().refreshStatus().state());
    }

    @Test
    void concurrentEnrichmentNeverExceedsConfiguredWorkerCount() throws Exception {
        List<RepositorySummary> discovered = List.of(
                repository(1L, "one"),
                repository(2L, "two"),
                repository(3L, "three"),
                repository(4L, "four"));

        RepositoryRefreshPlanner planner = org.mockito.Mockito.mock(RepositoryRefreshPlanner.class);
        org.mockito.Mockito.when(planner.plan(discovered))
                .thenReturn(new RepositoryRefreshPlan(
                        discovered.stream()
                                .map(item -> new RepositoryRefreshPlanItem(
                                        item,
                                        RepositoryRefreshAction.FULL_ENRICHMENT,
                                        null))
                                .toList(),
                        0,
                        0,
                        4,
                        4));

        AtomicInteger active = new AtomicInteger();
        AtomicInteger maxActive = new AtomicInteger();
        CountDownLatch firstTwoStarted = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);

        RepositoryEnrichmentService enrichment = item -> {
            int current = active.incrementAndGet();
            maxActive.accumulateAndGet(current, Math::max);
            firstTwoStarted.countDown();
            try {
                if (!release.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("timed out waiting for test release");
                }
                return complete(item);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(exception);
            } finally {
                active.decrementAndGet();
            }
        };

        var service = new InMemoryRepositoryInventoryService(
                () -> discovered,
                enrichment,
                CLOCK,
                null,
                null,
                null,
                null,
                null,
                planner,
                2);

        var executor = Executors.newSingleThreadExecutor();
        try {
            var future = executor.submit(service::refresh);

            assertTrue(firstTwoStarted.await(1, TimeUnit.SECONDS));
            assertEquals(2, maxActive.get());

            release.countDown();
            InventoryStatus completed = future.get(5, TimeUnit.SECONDS);

            assertEquals(InventoryRefreshState.COMPLETED, completed.state());
            assertEquals(4, completed.processedCount());
            assertEquals(4, completed.successfulCount());
            assertEquals(0, completed.errorCount());
            assertTrue(maxActive.get() <= 2);
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void concurrentEnrichmentIsolatesIndividualRepositoryFailure() {
        List<RepositorySummary> discovered = List.of(
                repository(1L, "one"),
                repository(2L, "two"),
                repository(3L, "three"));

        RepositoryRefreshPlanner planner = org.mockito.Mockito.mock(RepositoryRefreshPlanner.class);
        org.mockito.Mockito.when(planner.plan(discovered))
                .thenReturn(new RepositoryRefreshPlan(
                        discovered.stream()
                                .map(item -> new RepositoryRefreshPlanItem(
                                        item,
                                        RepositoryRefreshAction.FULL_ENRICHMENT,
                                        null))
                                .toList(),
                        0,
                        0,
                        3,
                        3));

        var service = new InMemoryRepositoryInventoryService(
                () -> discovered,
                item -> {
                    if (item.id() == 2L) {
                        throw new IllegalStateException("metadata unavailable");
                    }
                    return complete(item);
                },
                CLOCK,
                null,
                null,
                null,
                null,
                null,
                planner,
                2);

        InventoryStatus completed = service.refresh();

        assertEquals(InventoryRefreshState.PARTIAL, completed.state());
        assertEquals(3, completed.processedCount());
        assertEquals(2, completed.successfulCount());
        assertEquals(1, completed.errorCount());
        assertEquals(
                AnalysisState.FAILED,
                service.listRepositories().stream()
                        .filter(item -> item.id() == 2L)
                        .findFirst()
                        .orElseThrow()
                        .refreshStatus()
                        .state());
    }

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
    void successfulRefreshRemovesRepositoriesNoLongerInInstallation() {
        AtomicInteger calls = new AtomicInteger();
        GitHubRepositoryDiscoveryService discovery = () -> calls.getAndIncrement() == 0
                ? List.of(repository(1L, "one"), repository(2L, "two"))
                : List.of(repository(1L, "one"));

        var service = new InMemoryRepositoryInventoryService(discovery, this::complete, CLOCK);
        service.refresh();
        assertEquals(2, service.listRepositories().size());

        service.refresh();

        assertEquals(1, service.listRepositories().size());
        assertEquals(1L, service.listRepositories().getFirst().id());
        assertEquals(InventoryRefreshState.COMPLETED, service.getStatus().state());
    }

    @Test
    void individualEnrichmentFailureDoesNotFailWholeRefresh() {
        GitHubRepositoryDiscoveryService discovery = () -> List.of(repository(1L, "one"), repository(2L, "two"));
        RepositoryEnrichmentService enrichment = repository -> {
            if (repository.id() == 2L) {
                throw new IllegalStateException("metadata unavailable");
            }
            return complete(repository);
        };

        var service = new InMemoryRepositoryInventoryService(discovery, enrichment, CLOCK);
        service.refresh();

        assertEquals(InventoryRefreshState.PARTIAL, service.getStatus().state());
        assertEquals(2, service.listRepositories().size());
        assertEquals(2, service.getStatus().totalCount());
        assertEquals(2, service.getStatus().processedCount());
        assertEquals(1, service.getStatus().successfulCount());
        assertEquals(1, service.getStatus().errorCount());
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

    @Test
    void exposesProgressWhileRepositoriesAreBeingEnriched() {
        GitHubRepositoryDiscoveryService discovery =
                () -> List.of(repository(1L, "one"), repository(2L, "two"));
        final InMemoryRepositoryInventoryService[] holder = new InMemoryRepositoryInventoryService[1];
        AtomicInteger enrichedCount = new AtomicInteger();

        RepositoryEnrichmentService enrichment = repository -> {
            InventoryStatus current = holder[0].getStatus();
            assertEquals(InventoryRefreshState.RUNNING, current.state());
            assertEquals(2, current.totalCount());
            assertEquals(enrichedCount.get(), current.processedCount());
            assertEquals(repository.fullName(), current.currentRepository());
            enrichedCount.incrementAndGet();
            return complete(repository);
        };

        holder[0] = new InMemoryRepositoryInventoryService(discovery, enrichment, CLOCK);
        holder[0].refresh();

        assertEquals(InventoryRefreshState.COMPLETED, holder[0].getStatus().state());
        assertEquals(2, holder[0].getStatus().processedCount());
        assertEquals(2, holder[0].getStatus().successfulCount());
        assertEquals(0, holder[0].getStatus().errorCount());
    }


    @Test
    void initializationPublishesPersistedCacheBeforeGitHubRefreshCompletes() throws Exception {
        CountDownLatch discoveryStarted = new CountDownLatch(1);
        CountDownLatch allowDiscoveryToFinish = new CountDownLatch(1);
        GitHubRepositoryDiscoveryService discovery = () -> {
            discoveryStarted.countDown();
            try {
                if (!allowDiscoveryToFinish.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("timed out waiting for test release");
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(exception);
            }
            return List.of(repository(2L, "fresh"));
        };

        CachedRepositoryInventoryService cachedInventory =
                org.mockito.Mockito.mock(CachedRepositoryInventoryService.class);
        org.mockito.Mockito.when(cachedInventory.loadActiveRepositories())
                .thenReturn(List.of(repository(1L, "cached")));

        var executor = Executors.newSingleThreadExecutor();
        var service = new InMemoryRepositoryInventoryService(
                discovery,
                this::complete,
                CLOCK,
                executor,
                null,
                cachedInventory,
                null,
                null);
        try {
            service.initialize();

            assertTrue(discoveryStarted.await(1, TimeUnit.SECONDS));
            assertEquals(InventoryRefreshState.RUNNING, service.getStatus().state());
            assertEquals(1, service.listRepositories().size());
            assertEquals("erland/cached", service.listRepositories().getFirst().fullName());
        } finally {
            allowDiscoveryToFinish.countDown();
            service.shutdown();
        }
    }

    @Test
    void initializationStartsRefreshAsynchronouslyInsteadOfBlockingFirstApiUse() throws Exception {
        CountDownLatch discoveryStarted = new CountDownLatch(1);
        CountDownLatch allowDiscoveryToFinish = new CountDownLatch(1);
        GitHubRepositoryDiscoveryService discovery = () -> {
            discoveryStarted.countDown();
            try {
                if (!allowDiscoveryToFinish.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("timed out waiting for test release");
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(exception);
            }
            return List.of(repository(1L, "one"));
        };

        var executor = Executors.newSingleThreadExecutor();
        var service = new InMemoryRepositoryInventoryService(discovery, this::complete, CLOCK, executor);
        try {
            service.initialize();

            assertTrue(discoveryStarted.await(1, TimeUnit.SECONDS));
            assertEquals(InventoryRefreshState.RUNNING, service.getStatus().state());
            assertTrue(service.listRepositories().isEmpty());
        } finally {
            allowDiscoveryToFinish.countDown();
            service.shutdown();
        }
    }

    @Test
    void persistsEachRepositoryBeforeNextEnrichmentCompletes() throws Exception {
        CountDownLatch secondEnrichmentStarted = new CountDownLatch(1);
        CountDownLatch allowSecondEnrichmentToFinish = new CountDownLatch(1);
        GitHubRepositoryDiscoveryService discovery =
                () -> List.of(repository(1L, "one"), repository(2L, "two"));

        RepositoryEnrichmentService enrichment = repository -> {
            if (repository.id() == 2L) {
                secondEnrichmentStarted.countDown();
                try {
                    if (!allowSecondEnrichmentToFinish.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("timed out waiting for test release");
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(exception);
                }
            }
            return complete(repository);
        };

        RepositoryEnrichmentSnapshotService snapshots =
                org.mockito.Mockito.mock(RepositoryEnrichmentSnapshotService.class);
        var executor = Executors.newSingleThreadExecutor();
        var service = new InMemoryRepositoryInventoryService(
                discovery,
                enrichment,
                CLOCK,
                executor,
                null,
                null,
                snapshots,
                null);
        try {
            service.startRefresh();
            assertTrue(secondEnrichmentStarted.await(1, TimeUnit.SECONDS));

            org.mockito.Mockito.verify(snapshots, org.mockito.Mockito.times(1))
                    .persistProgressiveResult(
                            org.mockito.ArgumentMatchers.argThat(summary -> summary.id() == 1L),
                            org.mockito.ArgumentMatchers.eq(CLOCK.instant()));
            org.mockito.Mockito.verify(snapshots, org.mockito.Mockito.never())
                    .persistProgressiveResult(
                            org.mockito.ArgumentMatchers.argThat(summary -> summary.id() == 2L),
                            org.mockito.ArgumentMatchers.any());
        } finally {
            allowSecondEnrichmentToFinish.countDown();
            service.shutdown();
        }
    }

    @Test
    void publishesRepositoriesProgressivelyWhileEnrichmentContinues() throws Exception {
        CountDownLatch secondEnrichmentStarted = new CountDownLatch(1);
        CountDownLatch allowSecondEnrichmentToFinish = new CountDownLatch(1);
        GitHubRepositoryDiscoveryService discovery =
                () -> List.of(repository(1L, "one"), repository(2L, "two"));
        RepositoryEnrichmentService enrichment = repository -> {
            if (repository.id() == 2L) {
                secondEnrichmentStarted.countDown();
                try {
                    if (!allowSecondEnrichmentToFinish.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("timed out waiting for test release");
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(exception);
                }
            }
            return complete(repository);
        };

        var executor = Executors.newSingleThreadExecutor();
        var service = new InMemoryRepositoryInventoryService(discovery, enrichment, CLOCK, executor);
        try {
            service.startRefresh();
            assertTrue(secondEnrichmentStarted.await(1, TimeUnit.SECONDS));

            List<RepositorySummary> snapshot = service.listRepositories();
            assertEquals(2, snapshot.size());
            assertEquals(
                    info.isaksson.erland.repofleet.repository.api.AnalysisState.COMPLETE,
                    snapshot.get(0).refreshStatus().state());
            assertEquals(
                    info.isaksson.erland.repofleet.repository.api.AnalysisState.NOT_ANALYZED,
                    snapshot.get(1).refreshStatus().state());
            assertEquals(1, service.getStatus().processedCount());
        } finally {
            allowSecondEnrichmentToFinish.countDown();
            service.shutdown();
        }
    }

    @Test
    void allRepositoryEnrichmentFailuresProduceFailedRefresh() {
        GitHubRepositoryDiscoveryService discovery =
                () -> List.of(repository(1L, "one"), repository(2L, "two"));
        RepositoryEnrichmentService enrichment = repository -> {
            throw new IllegalStateException("metadata unavailable");
        };

        var service = new InMemoryRepositoryInventoryService(discovery, enrichment, CLOCK);
        service.refresh();

        assertEquals(InventoryRefreshState.FAILED, service.getStatus().state());
        assertEquals(2, service.getStatus().processedCount());
        assertEquals(0, service.getStatus().successfulCount());
        assertEquals(2, service.getStatus().errorCount());
    }

    @Test
    void completeRepositoriesProduceCompletedRefresh() {
        GitHubRepositoryDiscoveryService discovery =
                () -> List.of(repository(1L, "one"), repository(2L, "two"));
        var service = new InMemoryRepositoryInventoryService(discovery, this::complete, CLOCK);

        service.refresh();

        assertEquals(InventoryRefreshState.COMPLETED, service.getStatus().state());
        assertEquals(2, service.getStatus().successfulCount());
        assertEquals(0, service.getStatus().errorCount());
        assertTrue(service.getStatus().completedAt() != null);
    }

    private RepositorySummary complete(RepositorySummary repository) {
        return new RepositorySummary(
                repository.id(),
                repository.owner(),
                repository.name(),
                repository.fullName(),
                repository.url(),
                repository.visibility(),
                repository.archived(),
                repository.fork(),
                repository.defaultBranch(),
                repository.topics(),
                repository.languages(),
                repository.primaryLanguage(),
                repository.license(),
                repository.githubActions(),
                repository.release(),
                repository.activity(),
                new info.isaksson.erland.repofleet.repository.api.RepositoryRefreshStatus(
                        info.isaksson.erland.repofleet.repository.api.AnalysisState.COMPLETE,
                        "complete"));
    }


}
