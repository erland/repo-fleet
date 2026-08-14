package info.isaksson.erland.repofleet.repository.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class RepositorySummarySerializationTest {

    @Inject
    ObjectMapper objectMapper;

    @Test
    void serializesRepositorySummaryWithoutGitHubTransportTypes() throws Exception {
        var repository = new RepositorySummary(
            1L,
            "erland",
            "repo-fleet",
            "erland/repo-fleet",
            "https://github.com/erland/repo-fleet",
            RepositoryVisibility.PRIVATE,
            false,
            false,
            "main",
            List.of(),
            List.of(),
            null,
            new LicenseStatus(AnalysisState.NOT_ANALYZED, LicensePresence.UNKNOWN, null, null, null),
            new GitHubActionsStatus(AnalysisState.NOT_ANALYZED, null, null),
            new ReleaseStatus(AnalysisState.NOT_ANALYZED, null, null, null, null, null),
            new ActivityStatus(Instant.parse("2026-08-13T12:00:00Z"), Instant.parse("2026-08-13T12:05:00Z")),
            new RepositoryRefreshStatus(AnalysisState.NOT_ANALYZED, "Repository discovered; maintenance enrichment pending.")
        );

        String json = objectMapper.writeValueAsString(repository);
        var decoded = objectMapper.readTree(json);

        assertEquals("erland/repo-fleet", decoded.get("fullName").asText());
        assertEquals("UNKNOWN", decoded.get("license").get("presence").asText());
        assertEquals("NOT_ANALYZED", decoded.get("githubActions").get("analysisState").asText());
        assertTrue(decoded.get("topics").isArray());
    }
}
