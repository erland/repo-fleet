package info.isaksson.erland.repofleet.repository.refresh;

import info.isaksson.erland.repofleet.repository.api.CacheFreshness;
import info.isaksson.erland.repofleet.repository.persistence.RepositoryEnrichmentSnapshot;
import info.isaksson.erland.repofleet.repository.persistence.RepositoryIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class RepositoryRefreshPolicy {

    private final Duration identityFreshness;
    private final Duration enrichmentFreshness;
    private final Duration topicsFreshness;
    private final Duration languagesFreshness;
    private final Duration licenseFreshness;
    private final Duration workflowsFreshness;
    private final Duration releasesFreshness;
    private final Duration fullConsistencyInterval;

    @Inject
    public RepositoryRefreshPolicy(
            @ConfigProperty(name = "repofleet.refresh.identity-freshness-minutes", defaultValue = "15")
                    long identityFreshnessMinutes,
            @ConfigProperty(name = "repofleet.refresh.enrichment-freshness-minutes", defaultValue = "60")
                    long enrichmentFreshnessMinutes,
            @ConfigProperty(name = "repofleet.refresh.topics-freshness-minutes", defaultValue = "60")
                    long topicsFreshnessMinutes,
            @ConfigProperty(name = "repofleet.refresh.languages-freshness-minutes", defaultValue = "60")
                    long languagesFreshnessMinutes,
            @ConfigProperty(name = "repofleet.refresh.license-freshness-minutes", defaultValue = "240")
                    long licenseFreshnessMinutes,
            @ConfigProperty(name = "repofleet.refresh.workflows-freshness-minutes", defaultValue = "60")
                    long workflowsFreshnessMinutes,
            @ConfigProperty(name = "repofleet.refresh.releases-freshness-minutes", defaultValue = "60")
                    long releasesFreshnessMinutes,
            @ConfigProperty(name = "repofleet.refresh.full-consistency-hours", defaultValue = "24")
                    long fullConsistencyHours) {
        this.identityFreshness = Duration.ofMinutes(Math.max(1, identityFreshnessMinutes));
        this.enrichmentFreshness = Duration.ofMinutes(Math.max(1, enrichmentFreshnessMinutes));
        this.topicsFreshness = Duration.ofMinutes(Math.max(1, topicsFreshnessMinutes));
        this.languagesFreshness = Duration.ofMinutes(Math.max(1, languagesFreshnessMinutes));
        this.licenseFreshness = Duration.ofMinutes(Math.max(1, licenseFreshnessMinutes));
        this.workflowsFreshness = Duration.ofMinutes(Math.max(1, workflowsFreshnessMinutes));
        this.releasesFreshness = Duration.ofMinutes(Math.max(1, releasesFreshnessMinutes));
        this.fullConsistencyInterval = Duration.ofHours(Math.max(1, fullConsistencyHours));
    }

    public RepositoryRefreshPolicy(
            Duration identityFreshness,
            Duration enrichmentFreshness,
            Duration fullConsistencyInterval) {
        this(
                identityFreshness,
                enrichmentFreshness,
                enrichmentFreshness,
                enrichmentFreshness,
                enrichmentFreshness,
                enrichmentFreshness,
                enrichmentFreshness,
                fullConsistencyInterval);
    }

    public RepositoryRefreshPolicy(
            Duration identityFreshness,
            Duration enrichmentFreshness,
            Duration topicsFreshness,
            Duration languagesFreshness,
            Duration licenseFreshness,
            Duration workflowsFreshness,
            Duration releasesFreshness,
            Duration fullConsistencyInterval) {
        this.identityFreshness = identityFreshness;
        this.enrichmentFreshness = enrichmentFreshness;
        this.topicsFreshness = topicsFreshness;
        this.languagesFreshness = languagesFreshness;
        this.licenseFreshness = licenseFreshness;
        this.workflowsFreshness = workflowsFreshness;
        this.releasesFreshness = releasesFreshness;
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

    public boolean categoryFresh(String resourceCategory, Instant lastSuccessfulFetchAt, Instant now) {
        if (lastSuccessfulFetchAt == null) {
            return false;
        }
        Duration freshness = switch (resourceCategory) {
            case "topics" -> topicsFreshness;
            case "languages" -> languagesFreshness;
            case "root-contents", "license" -> licenseFreshness;
            case "workflows" -> workflowsFreshness;
            case "releases" -> releasesFreshness;
            default -> enrichmentFreshness;
        };
        return !lastSuccessfulFetchAt.isBefore(now.minus(freshness));
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
