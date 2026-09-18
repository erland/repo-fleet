package info.isaksson.erland.repofleet.repository.refresh;

import info.isaksson.erland.repofleet.repository.inventory.InMemoryRepositoryInventoryService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class RepositoryConsistencyRefreshScheduler {

    private final InMemoryRepositoryInventoryService inventory;
    private final boolean enabled;
    private final long intervalHours;
    private ScheduledExecutorService scheduler;

    @Inject
    public RepositoryConsistencyRefreshScheduler(
            InMemoryRepositoryInventoryService inventory,
            @ConfigProperty(name = "repofleet.refresh.consistency-scheduler-enabled", defaultValue = "true")
                    boolean enabled,
            @ConfigProperty(name = "repofleet.refresh.consistency-interval-hours", defaultValue = "24")
                    long intervalHours) {
        this.inventory = inventory;
        this.enabled = enabled;
        this.intervalHours = Math.max(1, intervalHours);
    }

    @PostConstruct
    void start() {
        if (!enabled) return;

        scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "repo-fleet-consistency-refresh");
            thread.setDaemon(true);
            return thread;
        });

        scheduler.scheduleWithFixedDelay(
                this::runSafely,
                intervalHours,
                intervalHours,
                TimeUnit.HOURS);
    }

    void runOnce() {
        inventory.startScheduledConsistencyRefresh();
    }

    private void runSafely() {
        try {
            runOnce();
        } catch (RuntimeException ignored) {
            // The existing refresh history/status captures operational failures.
        }
    }

    @PreDestroy
    void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }
}
