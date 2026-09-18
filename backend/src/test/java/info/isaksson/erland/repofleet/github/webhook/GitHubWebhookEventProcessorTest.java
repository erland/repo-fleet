package info.isaksson.erland.repofleet.github.webhook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import info.isaksson.erland.repofleet.repository.api.RepositoryVisibility;
import info.isaksson.erland.repofleet.repository.persistence.RepositoryEnrichmentSnapshot;
import info.isaksson.erland.repofleet.repository.persistence.RepositoryEnrichmentSnapshotRepository;
import info.isaksson.erland.repofleet.repository.persistence.RepositoryIdentityRepository;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class GitHubWebhookEventProcessorTest {

    @Inject
    GitHubWebhookEventProcessor processor;

    @Inject
    RepositoryIdentityRepository identities;

    @Inject
    RepositoryEnrichmentSnapshotRepository snapshots;

    @BeforeEach
    @Transactional
    void clearDatabase() {
        GitHubWebhookDelivery.deleteAll();
        snapshots.deleteAll();
        identities.deleteAll();
    }

    @Test
    @Transactional
    void updatesRepositoryMetadataAndMarksEnrichmentStale() {
        Instant before = Instant.parse("2026-09-18T10:00:00Z");
        identities.insert(
                42L,
                "old-owner",
                "old-name",
                "old-owner/old-name",
                RepositoryVisibility.PRIVATE,
                false,
                false,
                "main",
                before,
                before,
                before);

        RepositoryEnrichmentSnapshot snapshot = new RepositoryEnrichmentSnapshot();
        snapshot.githubRepositoryId = 42L;
        snapshot.topicsJson = "[]";
        snapshot.languagesJson = "[]";
        snapshot.licenseAnalysisState = "COMPLETE";
        snapshot.licensePresence = "PRESENT";
        snapshot.actionsAnalysisState = "COMPLETE";
        snapshot.releaseAnalysisState = "COMPLETE";
        snapshot.enrichmentState = "COMPLETE";
        snapshot.lastSuccessfulRefreshAt = before;
        snapshot.updatedAt = before;
        snapshot.persist();

        Instant receivedAt = Instant.parse("2026-09-18T14:00:00Z");
        processor.process(
                "repository",
                """
                {
                  "action": "edited",
                  "repository": {
                    "id": 42,
                    "name": "new-name",
                    "full_name": "new-owner/new-name",
                    "owner": { "login": "new-owner" },
                    "visibility": "public",
                    "archived": true,
                    "fork": false,
                    "default_branch": "trunk",
                    "updated_at": "2026-09-18T13:59:00Z",
                    "pushed_at": "2026-09-18T13:58:00Z"
                  }
                }
                """,
                receivedAt);

        var identity = identities.findByGitHubRepositoryId(42L).orElseThrow();
        assertEquals("new-owner", identity.ownerLogin);
        assertEquals("new-name", identity.name);
        assertEquals("new-owner/new-name", identity.fullName);
        assertEquals(RepositoryVisibility.PUBLIC, identity.visibility);
        assertTrue(identity.archived);
        assertEquals("trunk", identity.defaultBranch);
        assertTrue(identity.active);
        assertEquals("LIKELY_CHANGED", identity.changeClassification);
        assertEquals(receivedAt, identity.changeDetectedAt);

        var updatedSnapshot = snapshots.findByGitHubRepositoryId(42L).orElseThrow();
        assertNull(updatedSnapshot.lastSuccessfulRefreshAt);
        assertEquals(receivedAt, updatedSnapshot.updatedAt);
        assertTrue(updatedSnapshot.enrichmentMessage.contains("refresh pending"));
    }

    @Test
    @Transactional
    void createsUnknownRepositoryFromCreatedEvent() {
        Instant receivedAt = Instant.parse("2026-09-18T14:00:00Z");
        processor.process(
                "repository",
                """
                {
                  "action": "created",
                  "repository": {
                    "id": 77,
                    "name": "brand-new",
                    "full_name": "erland/brand-new",
                    "owner": { "login": "erland" },
                    "private": true,
                    "archived": false,
                    "fork": false,
                    "default_branch": "main",
                    "updated_at": "2026-09-18T13:59:00Z",
                    "pushed_at": "2026-09-18T13:58:00Z"
                  }
                }
                """,
                receivedAt);

        var identity = identities.findByGitHubRepositoryId(77L).orElseThrow();
        assertEquals("erland/brand-new", identity.fullName);
        assertEquals(RepositoryVisibility.PRIVATE, identity.visibility);
        assertTrue(identity.active);
        assertEquals("LIKELY_CHANGED", identity.changeClassification);
    }

    @Test
    @Transactional
    void marksRepositoryInactiveOnDeletedEvent() {
        Instant before = Instant.parse("2026-09-18T10:00:00Z");
        identities.insert(
                99L,
                "erland",
                "to-delete",
                "erland/to-delete",
                RepositoryVisibility.PRIVATE,
                false,
                false,
                "main",
                before,
                before,
                before);

        Instant receivedAt = Instant.parse("2026-09-18T14:00:00Z");
        processor.process(
                "repository",
                """
                {
                  "action": "deleted",
                  "repository": { "id": 99 }
                }
                """,
                receivedAt);

        var identity = identities.findByGitHubRepositoryId(99L).orElseThrow();
        assertFalse(identity.active);
        assertEquals(receivedAt, identity.lastSeenAt);
        assertEquals("LIKELY_CHANGED", identity.changeClassification);
    }
}
