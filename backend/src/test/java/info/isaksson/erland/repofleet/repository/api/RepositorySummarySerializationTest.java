package info.isaksson.erland.repofleet.repository.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import info.isaksson.erland.repofleet.repository.inventory.SampleRepositoryInventoryService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class RepositorySummarySerializationTest {

    @Inject
    ObjectMapper objectMapper;

    @Test
    void serializesRepositorySummaryAsFrontendFriendlyJson() throws Exception {
        var repository = new SampleRepositoryInventoryService().listRepositories().get(0);
        var json = objectMapper.writeValueAsString(repository);
        var tree = objectMapper.readTree(json);

        assertEquals("roman-nollpunkten", tree.get("name").asText());
        assertEquals("PRIVATE", tree.get("visibility").asText());
        assertTrue(tree.get("topics").isArray());
        assertEquals("2026-08-10T17:30:00Z", tree.get("release").get("latestReleaseDate").asText());
    }
}
