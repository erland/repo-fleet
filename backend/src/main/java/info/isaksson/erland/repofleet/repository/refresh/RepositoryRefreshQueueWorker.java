package info.isaksson.erland.repofleet.repository.refresh;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class RepositoryRefreshQueueWorker {

    private final RepositoryRefreshQueueService queue;
    private final TargetedRepositoryRefreshService refresher;
    private final boolean enabled;
    private final int workerCount;
    private final Clock clock;
    private ScheduledExecutorService poller;
    private ExecutorService workers;
    private final AtomicInteger inFlight = new AtomicInteger();

    @Inject
    public RepositoryRefreshQueueWorker(
            RepositoryRefreshQueueService queue,
            TargetedRepositoryRefreshService refresher,
            @ConfigProperty(name = "repofleet.refresh.targeted-queue-enabled", defaultValue = "true")
                    boolean enabled,
            @ConfigProperty(name = "repofleet.refresh.targeted-workers", defaultValue = "2")
                    int workerCount) {
        this(queue, refresher, enabled, workerCount, Clock.systemUTC());
    }

    RepositoryRefreshQueueWorker(
            RepositoryRefreshQueueService queue,
            TargetedRepositoryRefreshService refresher,
            boolean enabled,
            int workerCount,
            Clock clock) {
        this.queue = queue;
        this.refresher = refresher;
        this.enabled = enabled;
        this.workerCount = Math.max(1, Math.min(8, workerCount));
        this.clock = clock;
    }

    @PostConstruct
    void start() {
        if (!enabled) return;

        Instant now = clock.instant();
        queue.recoverInterrupted(now);
        queue.enqueueStaleRepositories(now);

        workers = Executors.newFixedThreadPool(
                workerCount,
                runnable -> {
                    Thread thread = new Thread(runnable, "repo-fleet-targeted-refresh-worker");
                    thread.setDaemon(true);
                    return thread;
                });
        poller = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "repo-fleet-targeted-refresh-poller");
            thread.setDaemon(true);
            return thread;
        });
        poller.scheduleWithFixedDelay(this::pollSafely, 0, 1, TimeUnit.SECONDS);
    }

    private void pollSafely() {
        try {
            while (inFlight.get() < workerCount) {
                RepositoryRefreshJobView job = queue.claimNext(clock.instant());
                if (job == null) return;
                inFlight.incrementAndGet();
                workers.submit(() -> execute(job));
            }
        } catch (RuntimeException ignored) {
            // Keep the persistent queue alive; the next poll retries.
        }
    }

    private void execute(RepositoryRefreshJobView job) {
        try {
            refresher.refresh(job.githubRepositoryId(), clock.instant());
            queue.complete(job.id(), clock.instant());
        } catch (RuntimeException exception) {
            queue.fail(job.id(), safeMessage(exception), clock.instant());
        } finally {
            inFlight.decrementAndGet();
        }
    }

    private String safeMessage(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank()
                ? exception.getClass().getSimpleName()
                : message;
    }

    @PreDestroy
    void stop() {
        if (poller != null) poller.shutdownNow();
        if (workers != null) workers.shutdownNow();
    }
}
