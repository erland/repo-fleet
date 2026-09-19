package info.isaksson.erland.repofleet.repository.inventory;

import info.isaksson.erland.repofleet.repository.api.AnalysisState;
import info.isaksson.erland.repofleet.repository.api.CacheFreshness;
import info.isaksson.erland.repofleet.repository.api.RepositoryRefreshStatus;
import info.isaksson.erland.repofleet.repository.api.RepositorySummary;
import info.isaksson.erland.repofleet.repository.persistence.CachedRepositoryInventoryService;
import info.isaksson.erland.repofleet.repository.persistence.RepositoryInventoryPersistenceService;
import info.isaksson.erland.repofleet.repository.persistence.RepositoryEnrichmentSnapshotService;
import info.isaksson.erland.repofleet.repository.persistence.RepositoryRefreshHistoryService;
import info.isaksson.erland.repofleet.repository.refresh.RepositoryRefreshAction;
import info.isaksson.erland.repofleet.repository.refresh.RepositoryRefreshPlan;
import info.isaksson.erland.repofleet.repository.refresh.RepositoryRefreshPlanItem;
import info.isaksson.erland.repofleet.repository.refresh.RepositoryRefreshPlanner;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

@ApplicationScoped
public class InMemoryRepositoryInventoryService implements RepositoryInventoryService {

    private final GitHubRepositoryDiscoveryService discoveryService;
    private final RepositoryEnrichmentService enrichmentService;
    private final Clock clock;
    private final ExecutorService refreshExecutor;
    private final RepositoryInventoryPersistenceService persistenceService;
    private final CachedRepositoryInventoryService cachedInventoryService;
    private final RepositoryEnrichmentSnapshotService snapshotService;
    private final RepositoryRefreshHistoryService refreshHistoryService;
    private final RepositoryRefreshPlanner refreshPlanner;
    private final int enrichmentWorkers;
    private final ReentrantLock refreshLock = new ReentrantLock();

    private volatile List<RepositorySummary> repositories = List.of();
    private volatile InventoryStatus status = InventoryStatus.notStarted();
    private volatile int reusedCount;
    private volatile int newCount;
    private volatile int changedCount;
    private volatile int scheduledCount;

    @Inject
    public InMemoryRepositoryInventoryService(
            GitHubRepositoryDiscoveryService discoveryService,
            RepositoryEnrichmentService enrichmentService,
            RepositoryInventoryPersistenceService persistenceService,
            CachedRepositoryInventoryService cachedInventoryService,
            RepositoryEnrichmentSnapshotService snapshotService,
            RepositoryRefreshHistoryService refreshHistoryService,
            RepositoryRefreshPlanner refreshPlanner,
            @ConfigProperty(name = "repofleet.refresh.enrichment-workers", defaultValue = "2")
                    int enrichmentWorkers) {
        this(
                discoveryService,
                enrichmentService,
                Clock.systemUTC(),
                Executors.newSingleThreadExecutor(runnable -> {
                    Thread thread = new Thread(runnable, "repo-fleet-inventory-refresh");
                    thread.setDaemon(true);
                    return thread;
                }),
                persistenceService,
                cachedInventoryService,
                snapshotService,
                refreshHistoryService,
                refreshPlanner,
                enrichmentWorkers);
    }

    InMemoryRepositoryInventoryService(
            GitHubRepositoryDiscoveryService discoveryService,
            RepositoryEnrichmentService enrichmentService,
            Clock clock) {
        this(discoveryService, enrichmentService, clock, null, null, null, null, null, null, 1);
    }

    InMemoryRepositoryInventoryService(
            GitHubRepositoryDiscoveryService discoveryService,
            RepositoryEnrichmentService enrichmentService,
            Clock clock,
            ExecutorService refreshExecutor) {
        this(discoveryService, enrichmentService, clock, refreshExecutor, null, null, null, null, null, 1);
    }

    InMemoryRepositoryInventoryService(
            GitHubRepositoryDiscoveryService discoveryService,
            RepositoryEnrichmentService enrichmentService,
            Clock clock,
            ExecutorService refreshExecutor,
            RepositoryInventoryPersistenceService persistenceService,
            CachedRepositoryInventoryService cachedInventoryService,
            RepositoryEnrichmentSnapshotService snapshotService,
            RepositoryRefreshHistoryService refreshHistoryService) {
        this(
                discoveryService,
                enrichmentService,
                clock,
                refreshExecutor,
                persistenceService,
                cachedInventoryService,
                snapshotService,
                refreshHistoryService,
                null,
                1);
    }

    InMemoryRepositoryInventoryService(
            GitHubRepositoryDiscoveryService discoveryService,
            RepositoryEnrichmentService enrichmentService,
            Clock clock,
            ExecutorService refreshExecutor,
            RepositoryInventoryPersistenceService persistenceService,
            CachedRepositoryInventoryService cachedInventoryService,
            RepositoryEnrichmentSnapshotService snapshotService,
            RepositoryRefreshHistoryService refreshHistoryService,
            RepositoryRefreshPlanner refreshPlanner) {
        this(
                discoveryService,
                enrichmentService,
                clock,
                refreshExecutor,
                persistenceService,
                cachedInventoryService,
                snapshotService,
                refreshHistoryService,
                refreshPlanner,
                1);
    }

    InMemoryRepositoryInventoryService(
            GitHubRepositoryDiscoveryService discoveryService,
            RepositoryEnrichmentService enrichmentService,
            Clock clock,
            ExecutorService refreshExecutor,
            RepositoryInventoryPersistenceService persistenceService,
            CachedRepositoryInventoryService cachedInventoryService,
            RepositoryEnrichmentSnapshotService snapshotService,
            RepositoryRefreshHistoryService refreshHistoryService,
            RepositoryRefreshPlanner refreshPlanner,
            int enrichmentWorkers) {
        this.discoveryService = discoveryService;
        this.enrichmentService = enrichmentService;
        this.clock = clock;
        this.refreshExecutor = refreshExecutor;
        this.persistenceService = persistenceService;
        this.cachedInventoryService = cachedInventoryService;
        this.snapshotService = snapshotService;
        this.refreshHistoryService = refreshHistoryService;
        this.refreshPlanner = refreshPlanner;
        this.enrichmentWorkers = Math.max(1, Math.min(8, enrichmentWorkers));
    }

    @PostConstruct
    void initialize() {
        if (cachedInventoryService != null) {
            repositories = List.copyOf(cachedInventoryService.loadActiveRepositories());
        }
        startRefresh("AUTOMATIC");
    }

    @PreDestroy
    void shutdown() {
        if (refreshExecutor != null) {
            refreshExecutor.shutdownNow();
        }
    }

    @Override
    public List<RepositorySummary> listRepositories() {
        return repositories;
    }

    public synchronized void replaceRepository(RepositorySummary updated) {
        List<RepositorySummary> next = new ArrayList<>(repositories);
        boolean replaced = false;
        for (int index = 0; index < next.size(); index++) {
            if (next.get(index).id() == updated.id()) {
                next.set(index, updated);
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            next.add(updated);
        }
        next.sort(java.util.Comparator.comparing(
                RepositorySummary::fullName,
                String.CASE_INSENSITIVE_ORDER));
        repositories = List.copyOf(next);
    }

    @Override
    public InventoryStatus getStatus() {
        return status;
    }

    @Override
    public InventoryStatus startRefresh() {
        return startRefresh("MANUAL");
    }

    public InventoryStatus startScheduledConsistencyRefresh() {
        return startRefresh("SCHEDULED_CONSISTENCY");
    }

    private InventoryStatus startRefresh(String triggerType) {
        if (status.running()) {
            return status;
        }
        if (refreshExecutor == null) {
            return refreshFrom(clock.instant(), triggerType);
        }

        Instant startedAt = clock.instant();
        status = runningStatus(startedAt, 0, 0, 0, 0, null);
        refreshExecutor.submit(() -> refreshFrom(startedAt, triggerType));
        return status;
    }

    @Override
    public InventoryStatus refresh() {
        return refreshFrom(clock.instant(), "MANUAL");
    }

    private InventoryStatus refreshFrom(Instant startedAt, String triggerType) {
        if (!refreshLock.tryLock()) {
            return status;
        }

        Long refreshRunId = refreshHistoryService == null
                ? null
                : refreshHistoryService.startRun(triggerType, startedAt);

        try {
            reusedCount = 0;
            newCount = 0;
            changedCount = 0;
            scheduledCount = 0;
            status = runningStatus(startedAt, 0, 0, 0, 0, null);

            final List<RepositorySummary> discovered;
            try {
                discovered = List.copyOf(discoveryService.discoverRepositories());
            } catch (RuntimeException exception) {
                Instant completedAt = clock.instant();
                status = new InventoryStatus(
                        InventoryRefreshState.FAILED,
                        startedAt,
                        status.lastSuccessfulRefreshAt(),
                        completedAt,
                        safeMessage(exception),
                        repositories.size(),
                        0,
                        0,
                        0,
                        0,
                        reusedCount,
                        newCount,
                        changedCount,
                        scheduledCount,
                        null);
                completeHistory(refreshRunId, status);
                return status;
            }

            if (persistenceService != null) {
                try {
                    persistenceService.synchronize(discovered, clock.instant());
                } catch (RuntimeException exception) {
                    Instant completedAt = clock.instant();
                    status = new InventoryStatus(
                            InventoryRefreshState.FAILED,
                            startedAt,
                            status.lastSuccessfulRefreshAt(),
                            completedAt,
                            "Repository inventory persistence failed: " + safeMessage(exception),
                            repositories.size(),
                            discovered.size(),
                            0,
                            0,
                            0,
                            reusedCount,
                            newCount,
                            changedCount,
                            scheduledCount,
                            null);
                    completeHistory(refreshRunId, status);
                    return status;
                }
            }

            RepositoryRefreshPlan refreshPlan = refreshPlanner == null
                    ? null
                    : refreshPlanner.plan(discovered);
            if (refreshPlan != null) {
                reusedCount = refreshPlan.reusedCount();
                newCount = refreshPlan.newCount();
                changedCount = refreshPlan.changedCount();
                scheduledCount = refreshPlan.scheduledCount();
            }

            int total = discovered.size();
            int successful = 0;
            int errors = 0;
            int hardFailures = 0;

            List<RepositorySummary> working = refreshPlan == null
                    ? progressiveSnapshot(discovered)
                    : refreshPlan.items().stream()
                            .map(item -> item.action() == RepositoryRefreshAction.REUSE_CACHED
                                    ? item.cached()
                                    : withFreshness(item.discovered(), CacheFreshness.REFRESHING))
                            .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
            repositories = List.copyOf(working);
            status = runningStatus(startedAt, total, 0, 0, 0, null);

            ProcessingCounts counts =
                    refreshPlan != null && enrichmentWorkers > 1
                            ? processConcurrent(startedAt, refreshPlan, working)
                            : processSequential(startedAt, refreshPlan, discovered, working);
            successful = counts.successful();
            errors = counts.errors();
            hardFailures = counts.hardFailures();

            List<RepositorySummary> refreshed = List.copyOf(working);
            repositories = refreshed;
            Instant completedAt = clock.instant();
            InventoryRefreshState finalState =
                    errors == 0 ? InventoryRefreshState.COMPLETED
                            : hardFailures == total && total > 0 ? InventoryRefreshState.FAILED
                            : InventoryRefreshState.PARTIAL;

            Instant lastSuccessfulRefreshAt =
                    finalState == InventoryRefreshState.COMPLETED
                            ? completedAt
                            : status.lastSuccessfulRefreshAt();

            status = new InventoryStatus(
                    finalState,
                    startedAt,
                    lastSuccessfulRefreshAt,
                    completedAt,
                    errors == 0 ? null : errors + " repository enrichment(s) completed with errors.",
                    refreshed.size(),
                    total,
                    total,
                    successful,
                    errors,
                    reusedCount,
                    newCount,
                    changedCount,
                    scheduledCount,
                    null);
            completeHistory(refreshRunId, status);
            return status;
        } finally {
            refreshLock.unlock();
        }
    }

    private ProcessingCounts processSequential(
            Instant startedAt,
            RepositoryRefreshPlan refreshPlan,
            List<RepositorySummary> discovered,
            List<RepositorySummary> working) {
        int successful = 0;
        int errors = 0;
        int hardFailures = 0;

        for (int index = 0; index < discovered.size(); index++) {
            RepositoryRefreshPlanItem planItem =
                    refreshPlan == null ? null : refreshPlan.items().get(index);
            RepositorySummary repository =
                    planItem == null ? discovered.get(index) : planItem.discovered();
            status = runningStatus(
                    startedAt,
                    discovered.size(),
                    index,
                    successful,
                    errors,
                    repository.fullName());

            RepositorySummary enriched = processPlanItem(planItem, repository);
            working.set(index, enriched);
            repositories = List.copyOf(working);

            AnalysisState repositoryState = repositoryState(enriched);
            if (repositoryState == AnalysisState.COMPLETE) {
                successful++;
            } else {
                errors++;
                if (repositoryState == AnalysisState.FAILED) {
                    hardFailures++;
                }
            }

            status = runningStatus(
                    startedAt,
                    discovered.size(),
                    index + 1,
                    successful,
                    errors,
                    null);
        }
        return new ProcessingCounts(successful, errors, hardFailures);
    }

    private ProcessingCounts processConcurrent(
            Instant startedAt,
            RepositoryRefreshPlan refreshPlan,
            List<RepositorySummary> working) {
        int successful = 0;
        int errors = 0;
        int hardFailures = 0;
        int processed = 0;

        ExecutorService workers = Executors.newFixedThreadPool(
                enrichmentWorkers,
                runnable -> {
                    Thread thread = new Thread(runnable, "repo-fleet-enrichment-worker");
                    thread.setDaemon(true);
                    return thread;
                });
        CompletionService<IndexedEnrichmentResult> completion =
                new ExecutorCompletionService<>(workers);
        int submitted = 0;

        try {
            for (int index = 0; index < refreshPlan.items().size(); index++) {
                RepositoryRefreshPlanItem item = refreshPlan.items().get(index);
                if (item.action() == RepositoryRefreshAction.REUSE_CACHED) {
                    RepositorySummary reused = item.cached();
                    working.set(index, reused);
                    processed++;
                    AnalysisState state = repositoryState(reused);
                    if (state == AnalysisState.COMPLETE) {
                        successful++;
                    } else {
                        errors++;
                        if (state == AnalysisState.FAILED) {
                            hardFailures++;
                        }
                    }
                    continue;
                }

                final int resultIndex = index;
                completion.submit(() -> new IndexedEnrichmentResult(
                        resultIndex,
                        processPlanItemSafely(item, item.discovered())));
                submitted++;
            }

            repositories = List.copyOf(working);
            status = runningStatus(
                    startedAt,
                    refreshPlan.items().size(),
                    processed,
                    successful,
                    errors,
                    null);

            for (int completed = 0; completed < submitted; completed++) {
                IndexedEnrichmentResult result;
                try {
                    result = completion.take().get();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Repository enrichment was interrupted.", exception);
                } catch (java.util.concurrent.ExecutionException exception) {
                    throw new IllegalStateException(
                            "Repository enrichment worker failed unexpectedly.",
                            exception.getCause());
                }

                working.set(result.index(), result.repository());
                repositories = List.copyOf(working);
                processed++;

                AnalysisState state = repositoryState(result.repository());
                if (state == AnalysisState.COMPLETE) {
                    successful++;
                } else {
                    errors++;
                    if (state == AnalysisState.FAILED) {
                        hardFailures++;
                    }
                }

                status = runningStatus(
                        startedAt,
                        refreshPlan.items().size(),
                        processed,
                        successful,
                        errors,
                        null);
            }
        } finally {
            workers.shutdownNow();
        }

        return new ProcessingCounts(successful, errors, hardFailures);
    }

    private RepositorySummary processPlanItemSafely(
            RepositoryRefreshPlanItem planItem,
            RepositorySummary repository) {
        try {
            return processPlanItem(planItem, repository);
        } catch (RuntimeException exception) {
            return withFreshness(
                    new RepositorySummary(
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
                            new RepositoryRefreshStatus(
                                    AnalysisState.FAILED,
                                    "Repository enrichment processing failed: " + safeMessage(exception))),
                    CacheFreshness.STALE);
        }
    }

    private RepositorySummary processPlanItem(
            RepositoryRefreshPlanItem planItem,
            RepositorySummary repository) {
        if (planItem != null && planItem.action() == RepositoryRefreshAction.REUSE_CACHED) {
            return planItem.cached();
        }

        RepositorySummary enrichmentBase = enrichmentBase(planItem, repository);
        RepositorySummary enriched = enrichSafely(enrichmentBase);
        AnalysisState enrichedState = repositoryState(enriched);
        enriched = withFreshness(
                enriched,
                enrichedState == AnalysisState.COMPLETE
                        ? CacheFreshness.FRESH
                        : CacheFreshness.STALE);
        if (snapshotService != null) {
            snapshotService.persistProgressiveResult(enriched, clock.instant());
        }
        return enriched;
    }

    private RepositorySummary enrichmentBase(
            RepositoryRefreshPlanItem planItem,
            RepositorySummary discovered) {
        if (planItem == null || planItem.cached() == null) {
            return discovered;
        }

        RepositorySummary cached = planItem.cached();
        return new RepositorySummary(
                discovered.id(),
                discovered.owner(),
                discovered.name(),
                discovered.fullName(),
                discovered.url(),
                discovered.visibility(),
                discovered.archived(),
                discovered.fork(),
                discovered.defaultBranch(),
                cached.topics(),
                cached.languages(),
                cached.primaryLanguage(),
                cached.license(),
                cached.githubActions(),
                cached.release(),
                discovered.activity(),
                cached.refreshStatus());
    }

    private AnalysisState repositoryState(RepositorySummary repository) {
        return repository.refreshStatus() == null
                ? AnalysisState.FAILED
                : repository.refreshStatus().state();
    }

    private record ProcessingCounts(int successful, int errors, int hardFailures) {
    }

    private record IndexedEnrichmentResult(int index, RepositorySummary repository) {
    }

    private InventoryStatus runningStatus(
            Instant startedAt,
            int total,
            int processed,
            int successful,
            int errors,
            String currentRepository) {
        return new InventoryStatus(
                InventoryRefreshState.RUNNING,
                startedAt,
                status.lastSuccessfulRefreshAt(),
                null,
                null,
                repositories.size(),
                total,
                processed,
                successful,
                errors,
                reusedCount,
                newCount,
                changedCount,
                scheduledCount,
                currentRepository);
    }



    private List<RepositorySummary> progressiveSnapshot(List<RepositorySummary> discovered) {
        if (repositories.isEmpty()) {
            return new ArrayList<>(discovered);
        }

        java.util.Map<Long, RepositorySummary> previousById = repositories.stream()
                .collect(java.util.stream.Collectors.toMap(RepositorySummary::id, repository -> repository));
        List<RepositorySummary> snapshot = new ArrayList<>(discovered.size());
        for (RepositorySummary repository : discovered) {
            snapshot.add(previousById.getOrDefault(repository.id(), repository));
        }
        return snapshot;
    }

    private RepositorySummary withFreshness(
            RepositorySummary repository,
            CacheFreshness freshness) {
        RepositoryRefreshStatus current = repository.refreshStatus();
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
                new RepositoryRefreshStatus(
                        current == null ? AnalysisState.NOT_ANALYZED : current.state(),
                        current == null ? null : current.message(),
                        freshness));
    }

    private RepositorySummary enrichSafely(RepositorySummary repository) {
        try {
            return enrichmentService.enrich(repository);
        } catch (RuntimeException exception) {
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
                    new RepositoryRefreshStatus(
                            AnalysisState.FAILED,
                            "Repository enrichment failed: " + safeMessage(exception)));
        }
    }

    private void completeHistory(Long refreshRunId, InventoryStatus completedStatus) {
        if (refreshRunId == null || refreshHistoryService == null) {
            return;
        }
        refreshHistoryService.completeRun(
                refreshRunId,
                completedStatus.state(),
                completedStatus.completedAt(),
                completedStatus.totalCount(),
                completedStatus.processedCount(),
                completedStatus.successfulCount(),
                completedStatus.errorCount(),
                completedStatus.reusedCount(),
                completedStatus.scheduledCount(),
                completedStatus.errorMessage());
    }

    private String safeMessage(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank()
                ? exception.getClass().getSimpleName()
                : message;
    }
}
