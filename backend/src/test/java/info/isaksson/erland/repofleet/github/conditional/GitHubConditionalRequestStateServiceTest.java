package info.isaksson.erland.repofleet.github.conditional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import info.isaksson.erland.repofleet.repository.api.RepositoryVisibility;
import info.isaksson.erland.repofleet.repository.persistence.RepositoryIdentityRepository;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class GitHubConditionalRequestStateServiceTest {

    @Inject
    GitHubConditionalRequestStateService stateService;

    @Inject
    RepositoryIdentityRepository identityRepository;

    @BeforeEach
    @Transactional
    void clearDatabase() {
        GitHubConditionalRequestState.deleteAll();
        identityRepository.deleteAll();
        identityRepository.insert(
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
                Instant.parse("2026-09-18T08:00:00Z"));
    }

    @Test
    void storesAndRefreshesConditionalRequestState() {
        Instant firstFetch = Instant.parse("2026-09-18T08:05:00Z");
        Instant secondFetch = Instant.parse("2026-09-18T09:05:00Z");

        stateService.recordModified(1234L, "topics", "\"etag-1\"", firstFetch);
        stateService.recordNotModified(1234L, "topics", secondFetch);

        var state = stateService.find(1234L, "topics").orElseThrow();

        assertEquals("\"etag-1\"", state.etag);
        assertEquals(secondFetch, state.lastSuccessfulFetchAt);
        assertEquals(secondFetch, state.updatedAt);
        assertTrue(state.id != null);
    }
}
