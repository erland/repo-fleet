package info.isaksson.erland.repofleet.repository.refresh;

import info.isaksson.erland.repofleet.repository.api.CacheFreshness;
import info.isaksson.erland.repofleet.repository.persistence.RepositoryEnrichmentSnapshot;
import info.isaksson.erland.repofleet.repository.persistence.RepositoryIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Duration;
import java.time.Instant;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class RepositoryRefreshPolicy {

    private final Duration identityFreshness;
    private final Duration enrichmentFreshness;
    private final Duration fullConsistencyInterval;

    public RepositoryRefreshPolicy(
            @ConfigProperty(name = "repofleet.refresh.identity-freshness-minutes", defaultValue = "15")
                    long identityFreshnessMinutes,
            @ConfigProperty(name = "repofleet.refresh.enrichment-freshness-minutes", defaultValue = "60")
                    long enrichmentFreshnessMinutes,
            @ConfigProperty(name = "repofleet.refresh.full-consistency-hours", defaultValue = "24")
                    long fullConsistencyHours) {
        this.identityFreshness = Duration.ofMinutes(Math.max(1, identityFreshnessMinutes));
        this.enrichmentFreshness = Duration.ofMinutes(Math.max(1, enrichmentFreshnessMinutes));
        this.fullConsistencyInterval = Duration.ofHours(Math.max(1, fullConsistencyHours));
    }

    RepositoryRefreshPolicy(
            Duration identityFreshness,
            Duration enrichmentFreshness,
            Duration fullConsistencyInterval) {
        this.identityFreshness = identityFreshness;
        this.enrichmentFreshness = enrichmentFreshness;
        this.fullConsistencyInterval = fullConsistencyInterval;
    }

    public boolean identityFresh(RepositoryIdentity identity, Instant now) {
        return identity != null
                && identity.lastSeenAt != null
                && !identity.lastSeenAt.isBefore(now.minus(identityFreshness));
    }

    public boolean enrichmentFresh(RepositoryEnrichmentSnapshot snapshot, Instant now) {
        return snapshot != null
                && snapshot.lastSuccessfulRefreshAt != null
                && !snapshot.lastSuccessfulRefreshAt.isBefore(now.minus(enrichmentFreshness));
    }

    public boolean fullConsistencyDue(RepositoryEnrichmentSnapshot snapshot, Instant now) {
        return snapshot == null
                || snapshot.lastSuccessfulRefreshAt == null
                || snapshot.lastSuccessfulRefreshAt.isBefore(now.minus(fullConsistencyInterval));
    }

    public CacheFreshness freshness(
            RepositoryIdentity identity,
            RepositoryEnrichmentSnapshot snapshot,
            boolean refreshing,
            Instant now) {
        if (refreshing) {
            return CacheFreshness.REFRESHING;
        }
        return identityFresh(identity, now) && enrichmentFresh(snapshot, now)
                ? CacheFreshness.FRESH
                : CacheFreshness.STALE;
    }

    Duration identityFreshness() {
        return identityFreshness;
    }

    Duration enrichmentFreshness() {
        return enrichmentFreshness;
    }

    Duration fullConsistencyInterval() {
        return fullConsistencyInterval;
    }
}
