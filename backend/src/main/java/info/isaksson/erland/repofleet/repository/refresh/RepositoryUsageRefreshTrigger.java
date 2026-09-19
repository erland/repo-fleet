package info.isaksson.erland.repofleet.repository.refresh;

import info.isaksson.erland.repofleet.repository.inventory.InMemoryRepositoryInventoryService;
import info.isaksson.erland.repofleet.repository.persistence.RepositoryRefreshHistoryService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class RepositoryUsageRefreshTrigger {

    private final InMemoryRepositoryInventoryService inventory;
    private final RepositoryRefreshHistoryService history;
    private final Duration minimumInterval;
    private final Clock clock;

    @Inject
    public RepositoryUsageRefreshTrigger(
            InMemoryRepositoryInventoryService inventory,
            RepositoryRefreshHistoryService history,
            @ConfigProperty(name = "repofleet.refresh.usage-check-interval-minutes", defaultValue = "60")
                    long minimumIntervalMinutes) {
        this(inventory, history, Duration.ofMinutes(Math.max(1, minimumIntervalMinutes)), Clock.systemUTC());
    }

    RepositoryUsageRefreshTrigger(
            InMemoryRepositoryInventoryService inventory,
            RepositoryRefreshHistoryService history,
            Duration minimumInterval,
            Clock clock) {
        this.inventory = inventory;
        this.history = history;
        this.minimumInterval = minimumInterval;
        this.clock = clock;
    }

    public synchronized boolean onAuthenticatedUse() {
        if (inventory.getStatus().running()) {
            return false;
        }
        Instant now = clock.instant();
        Instant latestAttemptAt = history.latestAttemptAt();
        if (latestAttemptAt != null && !latestAttemptAt.isBefore(now.minus(minimumInterval))) {
            return false;
        }
        inventory.startUsageRefresh();
        return true;
    }
}
