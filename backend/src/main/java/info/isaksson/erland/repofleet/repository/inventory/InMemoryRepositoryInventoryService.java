package info.isaksson.erland.repofleet.repository.inventory;

import info.isaksson.erland.repofleet.repository.api.RepositorySummary;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@ApplicationScoped
public class InMemoryRepositoryInventoryService implements RepositoryInventoryService {

    private final GitHubRepositoryDiscoveryService discoveryService;
    private final RepositoryEnrichmentService enrichmentService;
    private final Clock clock;
    private final ReentrantLock refreshLock = new ReentrantLock();

    private volatile List<RepositorySummary> repositories = List.of();
    private volatile InventoryStatus status =
            new InventoryStatus(InventoryRefreshState.NOT_STARTED, null, null, null, 0);

    @Inject
    public InMemoryRepositoryInventoryService(
            GitHubRepositoryDiscoveryService discoveryService,
            RepositoryEnrichmentService enrichmentService) {
        this(discoveryService, enrichmentService, Clock.systemUTC());
    }

    InMemoryRepositoryInventoryService(
            GitHubRepositoryDiscoveryService discoveryService,
            RepositoryEnrichmentService enrichmentService,
            Clock clock) {
        this.discoveryService = discoveryService;
        this.enrichmentService = enrichmentService;
        this.clock = clock;
    }

    @PostConstruct
    void initialize() {
        refresh();
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
    public InventoryStatus refresh() {
        if (!refreshLock.tryLock()) {
            return status;
        }

        try {
            Instant startedAt = clock.instant();
            status = new InventoryStatus(
                    InventoryRefreshState.RUNNING,
                    startedAt,
                    status.lastSuccessfulRefreshAt(),
                    null,
                    repositories.size());

            try {
                List<RepositorySummary> discovered = discoveryService.discoverRepositories();
                List<RepositorySummary> refreshed = discovered.stream()
                        .map(this::enrichSafely)
                        .toList();
                repositories = List.copyOf(refreshed);
                Instant completedAt = clock.instant();
                status = new InventoryStatus(
                        InventoryRefreshState.COMPLETED,
                        startedAt,
                        completedAt,
                        null,
                        refreshed.size());
            } catch (RuntimeException exception) {
                status = new InventoryStatus(
                        InventoryRefreshState.FAILED,
                        startedAt,
                        status.lastSuccessfulRefreshAt(),
                        safeMessage(exception),
                        repositories.size());
            }

            return status;
        } finally {
            refreshLock.unlock();
        }
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
                    new info.isaksson.erland.repofleet.repository.api.RepositoryRefreshStatus(
                            info.isaksson.erland.repofleet.repository.api.AnalysisState.FAILED,
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
