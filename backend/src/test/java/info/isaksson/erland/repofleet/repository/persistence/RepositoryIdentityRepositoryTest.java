package info.isaksson.erland.repofleet.repository.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import info.isaksson.erland.repofleet.repository.api.RepositoryVisibility;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class RepositoryIdentityRepositoryTest {

    @Inject
    RepositoryIdentityRepository repository;

    @BeforeEach
    @Transactional
    void clearDatabase() {
        repository.deleteAll();
    }

    @Test
    @Transactional
    void insertsAndFindsIdentityByGitHubRepositoryId() {
        Instant seenAt = Instant.parse("2026-09-18T06:30:00Z");

        repository.insert(
                1234L,
                "erland",
                "repo-fleet",
                "erland/repo-fleet",
                RepositoryVisibility.PUBLIC,
                false,
                false,
                "main",
                Instant.parse("2026-09-17T03:48:40Z"),
                Instant.parse("2026-09-17T03:48:41Z"),
                seenAt);

        RepositoryIdentity stored = repository.findByGitHubRepositoryId(1234L).orElseThrow();

        assertEquals(1234L, stored.githubRepositoryId);
        assertEquals("erland", stored.ownerLogin);
        assertEquals("repo-fleet", stored.name);
        assertEquals("erland/repo-fleet", stored.fullName);
        assertEquals(RepositoryVisibility.PUBLIC, stored.visibility);
        assertEquals("main", stored.defaultBranch);
        assertEquals(seenAt, stored.firstSeenAt);
        assertEquals(seenAt, stored.lastSeenAt);
        assertTrue(stored.active);
    }

    @Test
    @Transactional
    void updatesMutableIdentityFieldsWithoutChangingFirstSeen() {
        Instant firstSeen = Instant.parse("2026-09-18T06:30:00Z");
        Instant lastSeen = Instant.parse("2026-09-18T07:30:00Z");

        repository.insert(
                1234L,
                "erland",
                "repo-fleet",
                "erland/repo-fleet",
                RepositoryVisibility.PUBLIC,
                false,
                false,
                "main",
                null,
                null,
                firstSeen);

        repository.update(
                1234L,
                "erland",
                "repo-fleet-renamed",
                "erland/repo-fleet-renamed",
                RepositoryVisibility.PRIVATE,
                true,
                false,
                "trunk",
                Instant.parse("2026-09-18T07:00:00Z"),
                Instant.parse("2026-09-18T07:15:00Z"),
                lastSeen,
                false);

        RepositoryIdentity updated = repository.findByGitHubRepositoryId(1234L).orElseThrow();

        assertEquals("repo-fleet-renamed", updated.name);
        assertEquals("erland/repo-fleet-renamed", updated.fullName);
        assertEquals(RepositoryVisibility.PRIVATE, updated.visibility);
        assertTrue(updated.archived);
        assertEquals("trunk", updated.defaultBranch);
        assertEquals(firstSeen, updated.firstSeenAt);
        assertEquals(lastSeen, updated.lastSeenAt);
        assertFalse(updated.active);
    }

    @Test
    @Transactional
    void githubRepositoryIdIsUnique() {
        Instant seenAt = Instant.parse("2026-09-18T06:30:00Z");

        repository.insert(
                1234L,
                "erland",
                "one",
                "erland/one",
                RepositoryVisibility.PUBLIC,
                false,
                false,
                "main",
                null,
                null,
                seenAt);

        repository.insert(
                1234L,
                "erland",
                "two",
                "erland/two",
                RepositoryVisibility.PUBLIC,
                false,
                false,
                "main",
                null,
                null,
                seenAt);

        assertThrows(RuntimeException.class, repository::flush);
    }

    @Test
    @Transactional
    void updateFailsForUnknownGitHubRepositoryId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> repository.update(
                        9999L,
                        "erland",
                        "missing",
                        "erland/missing",
                        RepositoryVisibility.PRIVATE,
                        false,
                        false,
                        "main",
                        null,
                        null,
                        Instant.parse("2026-09-18T06:30:00Z"),
                        true));
    }
}
