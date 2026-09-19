package info.isaksson.erland.repofleet.repository.refresh;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import info.isaksson.erland.repofleet.repository.inventory.InMemoryRepositoryInventoryService;
import info.isaksson.erland.repofleet.repository.inventory.InventoryRefreshState;
import info.isaksson.erland.repofleet.repository.inventory.InventoryStatus;
import info.isaksson.erland.repofleet.repository.persistence.RepositoryRefreshHistoryService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class RepositoryUsageRefreshTriggerTest {

    private static final Instant NOW = Instant.parse("2026-09-19T05:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void startsRefreshWhenLastAttemptIsOlderThanMinimumInterval() {
        var inventory = mock(InMemoryRepositoryInventoryService.class);
        var history = mock(RepositoryRefreshHistoryService.class);
        when(inventory.getStatus()).thenReturn(InventoryStatus.cached(10));
        when(history.latestAttemptAt()).thenReturn(NOW.minus(Duration.ofHours(2)));
        var trigger = new RepositoryUsageRefreshTrigger(inventory, history, Duration.ofMinutes(60), CLOCK);

        assertTrue(trigger.onAuthenticatedUse());
        verify(inventory).startUsageRefresh();
    }

    @Test
    void skipsRefreshWhenRecentAttemptExists() {
        var inventory = mock(InMemoryRepositoryInventoryService.class);
        var history = mock(RepositoryRefreshHistoryService.class);
        when(inventory.getStatus()).thenReturn(InventoryStatus.cached(10));
        when(history.latestAttemptAt()).thenReturn(NOW.minus(Duration.ofMinutes(30)));
        var trigger = new RepositoryUsageRefreshTrigger(inventory, history, Duration.ofMinutes(60), CLOCK);

        assertFalse(trigger.onAuthenticatedUse());
        verify(history).latestAttemptAt();
    }

    @Test
    void skipsRefreshWhileRefreshIsAlreadyRunning() {
        var inventory = mock(InMemoryRepositoryInventoryService.class);
        var history = mock(RepositoryRefreshHistoryService.class);
        when(inventory.getStatus()).thenReturn(new InventoryStatus(
                InventoryRefreshState.RUNNING, NOW, null, null, null,
                10, 10, 0, 0, 0, 0, 0, 0, 0, null));
        var trigger = new RepositoryUsageRefreshTrigger(inventory, history, Duration.ofMinutes(60), CLOCK);

        assertFalse(trigger.onAuthenticatedUse());
        verifyNoInteractions(history);
    }
}
