package info.isaksson.erland.repofleet.repository.refresh;

import info.isaksson.erland.repofleet.repository.persistence.RepositoryEnrichmentSnapshotRepository;
import info.isaksson.erland.repofleet.repository.persistence.RepositoryIdentityRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@ApplicationScoped
public class RepositoryRefreshQueueService {

    private final RepositoryIdentityRepository identities;
    private final RepositoryEnrichmentSnapshotRepository snapshots;
    private final RepositoryRefreshPolicy refreshPolicy;

    @Inject
    public RepositoryRefreshQueueService(
            RepositoryIdentityRepository identities,
            RepositoryEnrichmentSnapshotRepository snapshots,
            RepositoryRefreshPolicy refreshPolicy) {
        this.identities = identities;
        this.snapshots = snapshots;
        this.refreshPolicy = refreshPolicy;
    }

    @Transactional
    public RepositoryRefreshJobView enqueue(
            long repositoryId,
            String triggerType,
            Instant now) {
        if (identities.findByGitHubRepositoryId(repositoryId).isEmpty()) {
            throw new IllegalArgumentException("Unknown repository: " + repositoryId);
        }

        RepositoryRefreshJob active = findActiveJob(repositoryId);
        if (active != null) {
            return toView(active);
        }

        RepositoryRefreshJob job = new RepositoryRefreshJob();
        job.githubRepositoryId = repositoryId;
        job.triggerType = triggerType == null || triggerType.isBlank() ? "UNKNOWN" : triggerType;
        job.state = RepositoryRefreshJobState.PENDING;
        job.attempts = 0;
        job.maxAttempts = 3;
        job.nextAttemptAt = now;
        job.createdAt = now;
        job.updatedAt = now;
        job.persist();
        return toView(job);
    }

    /**
     * Claims the next due targeted-refresh job.
     *
     * <p>The supported runtime topology currently has one backend application instance. This
     * read-then-mutate claim is therefore safe for the supported deployment. Before backend
     * horizontal scaling is enabled, replace this with an atomic multi-consumer database claim
     * (for example PostgreSQL FOR UPDATE SKIP LOCKED or an equivalent conditional update) and
     * add concurrency tests proving that one job cannot be claimed twice.
     */
    @Transactional
    public RepositoryRefreshJobView claimNext(Instant now) {
        RepositoryRefreshJob job = RepositoryRefreshJob.find(
                        "(state = ?1 or state = ?2) and nextAttemptAt <= ?3 order by createdAt",
                        RepositoryRefreshJobState.PENDING,
                        RepositoryRefreshJobState.RETRY,
                        now)
                .firstResultOptional()
                .map(RepositoryRefreshJob.class::cast)
                .orElse(null);
        if (job == null) return null;

        job.state = RepositoryRefreshJobState.RUNNING;
        job.attempts++;
        job.updatedAt = now;
        return toView(job);
    }

    @Transactional
    public void complete(long jobId, Instant now) {
        RepositoryRefreshJob job = RepositoryRefreshJob.findById(jobId);
        if (job == null) return;
        job.state = RepositoryRefreshJobState.SUCCEEDED;
        job.completedAt = now;
        job.updatedAt = now;
        job.lastError = null;
    }

    @Transactional
    public void fail(long jobId, String error, Instant now) {
        RepositoryRefreshJob job = RepositoryRefreshJob.findById(jobId);
        if (job == null) return;
        job.lastError = error;
        job.updatedAt = now;
        if (job.attempts >= job.maxAttempts) {
            job.state = RepositoryRefreshJobState.FAILED;
            job.completedAt = now;
            return;
        }
        job.state = RepositoryRefreshJobState.RETRY;
        long backoffSeconds = Math.min(300, 5L * (1L << Math.max(0, job.attempts - 1)));
        job.nextAttemptAt = now.plusSeconds(backoffSeconds);
    }

    @Transactional
    public void recoverInterrupted(Instant now) {
        for (Object item : RepositoryRefreshJob.list(
                "state",
                RepositoryRefreshJobState.RUNNING)) {
            RepositoryRefreshJob job = (RepositoryRefreshJob) item;
            job.state = RepositoryRefreshJobState.RETRY;
            job.nextAttemptAt = now;
            job.updatedAt = now;
            job.lastError = "Recovered after application restart.";
        }
    }

    @Transactional
    public int enqueueStaleRepositories(Instant now) {
        int queued = 0;
        for (var identity : identities.list("active", true)) {
            var snapshot = snapshots.findByGitHubRepositoryId(identity.githubRepositoryId).orElse(null);
            if ((!refreshPolicy.identityFresh(identity, now) || !refreshPolicy.enrichmentFresh(snapshot, now))
                    && findActiveJob(identity.githubRepositoryId) == null) {
                enqueue(identity.githubRepositoryId, "STALE_CACHE", now);
                queued++;
            }
        }
        return queued;
    }

    public RepositoryRefreshJobView get(long jobId) {
        RepositoryRefreshJob job = RepositoryRefreshJob.findById(jobId);
        return job == null ? null : toView(job);
    }

    private RepositoryRefreshJob findActiveJob(long repositoryId) {
        return RepositoryRefreshJob.find(
                        "githubRepositoryId = ?1 and state in ?2",
                        repositoryId,
                        List.of(
                                RepositoryRefreshJobState.PENDING,
                                RepositoryRefreshJobState.RUNNING,
                                RepositoryRefreshJobState.RETRY))
                .firstResultOptional()
                .map(RepositoryRefreshJob.class::cast)
                .orElse(null);
    }

    private RepositoryRefreshJobView toView(RepositoryRefreshJob job) {
        return new RepositoryRefreshJobView(
                job.id,
                job.githubRepositoryId,
                job.triggerType,
                job.state,
                job.attempts,
                job.maxAttempts,
                job.nextAttemptAt,
                job.lastError,
                job.createdAt,
                job.updatedAt,
                job.completedAt);
    }
}
