package info.isaksson.erland.repofleet.repository.inventory;

import info.isaksson.erland.repofleet.repository.api.AnalysisState;
import info.isaksson.erland.repofleet.repository.api.LicensePresence;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SampleRepositoryInventoryServiceTest {

    private final SampleRepositoryInventoryService service = new SampleRepositoryInventoryService();

    @Test
    void containsCompleteAndPartialRepresentativeRecords() {
        var repositories = service.listRepositories();

        assertEquals(3, repositories.size());
        assertEquals(AnalysisState.COMPLETE, repositories.get(0).refreshStatus().state());
        assertEquals(AnalysisState.PARTIAL, repositories.get(2).refreshStatus().state());
        assertEquals(LicensePresence.UNKNOWN, repositories.get(2).license().presence());
    }

    @Test
    void sampleInventoryCannotBeModifiedByConsumers() {
        var repositories = service.listRepositories();
        assertThrows(UnsupportedOperationException.class, () -> repositories.clear());
        assertThrows(UnsupportedOperationException.class, () -> repositories.get(0).topics().add("unexpected"));
        assertFalse(repositories.get(0).topics().isEmpty());
    }
}
