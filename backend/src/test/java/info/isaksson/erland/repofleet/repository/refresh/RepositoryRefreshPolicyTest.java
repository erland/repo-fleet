package info.isaksson.erland.repofleet.repository.refresh;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import info.isaksson.erland.repofleet.repository.persistence.RepositoryEnrichmentSnapshot;
import info.isaksson.erland.repofleet.repository.persistence.RepositoryIdentity;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class RepositoryRefreshPolicyTest {

    private static final Instant NOW = Instant.parse("2026-09-18T09:00:00Z");

    @Test
    void distinguishesFreshAndStaleIdentityAndEnrichment() {
        RepositoryRefreshPolicy policy = new RepositoryRefreshPolicy(
                Duration.ofMinutes(15),
                Duration.ofMinutes(60),
                Duration.ofHours(24));

        RepositoryIdentity identity = new RepositoryIdentity();
        identity.lastSeenAt = NOW.minusSeconds(5 * 60);

        RepositoryEnrichmentSnapshot snapshot = new RepositoryEnrichmentSnapshot();
        snapshot.lastSuccessfulRefreshAt = NOW.minusSeconds(30 * 60);

        assertTrue(policy.identityFresh(identity, NOW));
        assertTrue(policy.enrichmentFresh(snapshot, NOW));
        assertFalse(policy.fullConsistencyDue(snapshot, NOW));

        identity.lastSeenAt = NOW.minusSeconds(20 * 60);
        snapshot.lastSuccessfulRefreshAt = NOW.minusSeconds(2 * 60 * 60);

        assertFalse(policy.identityFresh(identity, NOW));
        assertFalse(policy.enrichmentFresh(snapshot, NOW));
    }

    @Test
    void appliesCategorySpecificFreshness() {
        RepositoryRefreshPolicy policy = new RepositoryRefreshPolicy(
                Duration.ofMinutes(15),
                Duration.ofMinutes(60),
                Duration.ofMinutes(30),
                Duration.ofMinutes(45),
                Duration.ofMinutes(240),
                Duration.ofMinutes(60),
                Duration.ofMinutes(90),
                Duration.ofHours(24));

        Instant fetchedAt = NOW.minusSeconds(40 * 60);

        assertFalse(policy.categoryFresh("topics", fetchedAt, NOW));
        assertTrue(policy.categoryFresh("languages", fetchedAt, NOW));
        assertTrue(policy.categoryFresh("license", fetchedAt, NOW));
        assertTrue(policy.categoryFresh("workflows", fetchedAt, NOW));
        assertTrue(policy.categoryFresh("releases", fetchedAt, NOW));
    }
}
