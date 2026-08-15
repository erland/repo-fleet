package info.isaksson.erland.repofleet.repository.inventory;

import info.isaksson.erland.repofleet.repository.api.AnalysisState;
import info.isaksson.erland.repofleet.repository.api.RepositoryRefreshStatus;
import info.isaksson.erland.repofleet.repository.api.RepositorySummary;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

@ApplicationScoped
public class InMemoryRepositoryInventoryService implements RepositoryInventoryService {

    private final GitHubRepositoryDiscoveryService discoveryService;
    private final RepositoryEnrichmentService enrichmentService;
    private final Clock clock;
    private final ExecutorService refreshExecutor;
    private final ReentrantLock refreshLock = new ReentrantLock();

    private volatile List<RepositorySummary> repositories = List.of();
    private volatile InventoryStatus status = InventoryStatus.notStarted();

    @Inject
    public InMemoryRepositoryInventoryService(
            GitHubRepositoryDiscoveryService discoveryService,
            RepositoryEnrichmentService enrichmentService) {
        this(
                discoveryService,
                enrichmentService,
                Clock.systemUTC(),
                Executors.newSingleThreadExecutor(runnable -> {
                    Thread thread = new Thread(runnable, "repo-fleet-inventory-refresh");
                    thread.setDaemon(true);
                    return thread;
                }));
    }

    InMemoryRepositoryInventoryService(
            GitHubRepositoryDiscoveryService discoveryService,
            RepositoryEnrichmentService enrichmentService,
            Clock clock) {
        this(discoveryService, enrichmentService, clock, null);
    }

    InMemoryRepositoryInventoryService(
            GitHubRepositoryDiscoveryService discoveryService,
            RepositoryEnrichmentService enrichmentService,
            Clock clock,
            ExecutorService refreshExecutor) {
        this.discoveryService = discoveryService;
        this.enrichmentService = enrichmentService;
        this.clock = clock;
        this.refreshExecutor = refreshExecutor;
    }

    @PostConstruct
    void initialize() {
        startRefresh();
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

    @Override
    public InventoryStatus getStatus() {
        return status;
    }

    @Override
    public InventoryStatus startRefresh() {
        if (status.running()) {
            return status;
        }
        if (refreshExecutor == null) {
            return refresh();
        }

        Instant startedAt = clock.instant();
        status = runningStatus(startedAt, 0, 0, 0, 0, null);
        refreshExecutor.submit(() -> refreshFrom(startedAt));
        return status;
    }

    @Override
    public InventoryStatus refresh() {
        return refreshFrom(clock.instant());
    }

    private InventoryStatus refreshFrom(Instant startedAt) {
        if (!refreshLock.tryLock()) {
            return status;
        }

        try {
            status = runningStatus(startedAt, 0, 0, 0, 0, null);

            final List<RepositorySummary> discovered;
            try {
                discovered = List.copyOf(discoveryService.discoverRepositories());
            } catch (RuntimeException exception) {
                status = new InventoryStatus(
                        InventoryRefreshState.FAILED,
                        startedAt,
                        status.lastSuccessfulRefreshAt(),
                        clock.instant(),
                        safeMessage(exception),
                        repositories.size(),
                        0,
                        0,
                        0,
                        0,
                        null);
                return status;
            }

            int total = discovered.size();
            int successful = 0;
            int errors = 0;
            int hardFailures = 0;

            List<RepositorySummary> working = progressiveSnapshot(discovered);
            repositories = List.copyOf(working);
            status = runningStatus(startedAt, total, 0, 0, 0, null);

            for (int index = 0; index < discovered.size(); index++) {
                RepositorySummary repository = discovered.get(index);
                status = runningStatus(
                        startedAt,
                        total,
                        index,
                        successful,
                        errors,
                        repository.fullName());

                RepositorySummary enriched = enrichSafely(repository);
                working.set(index, enriched);
                repositories = List.copyOf(working);

                AnalysisState repositoryState = enriched.refreshStatus() == null
                        ? AnalysisState.FAILED
                        : enriched.refreshStatus().state();
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
                        total,
                        index + 1,
                        successful,
                        errors,
                        null);
            }

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
                    null);
            return status;
        } finally {
            refreshLock.unlock();
        }
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

    private String safeMessage(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank()
                ? exception.getClass().getSimpleName()
                : message;
    }
}
