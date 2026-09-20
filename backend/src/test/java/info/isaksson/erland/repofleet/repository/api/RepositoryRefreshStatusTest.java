package info.isaksson.erland.repofleet.repository.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class RepositoryRefreshStatusTest {

    @Test
    void supportsExplicitLatestRefreshOutcome() {
        RepositoryRefreshStatus status = new RepositoryRefreshStatus(
                AnalysisState.COMPLETE,
                "Cached metadata retained after a transient GitHub failure",
                CacheFreshness.STALE,
                RepositoryRefreshOutcome.DEGRADED);

        assertEquals(AnalysisState.COMPLETE, status.state());
        assertEquals(CacheFreshness.STALE, status.freshness());
        assertEquals(RepositoryRefreshOutcome.DEGRADED, status.latestOutcome());
    }

    @Test
    void legacyConstructorsDoNotInventAnOutcome() {
        assertNull(new RepositoryRefreshStatus(
                AnalysisState.COMPLETE,
                null,
                CacheFreshness.FRESH).latestOutcome());

        assertNull(new RepositoryRefreshStatus(
                AnalysisState.NOT_ANALYZED,
                null).latestOutcome());
    }
}
