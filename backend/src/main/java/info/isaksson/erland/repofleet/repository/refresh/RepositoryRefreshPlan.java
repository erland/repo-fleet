package info.isaksson.erland.repofleet.repository.refresh;

import java.util.List;

public record RepositoryRefreshPlan(
        List<RepositoryRefreshPlanItem> items,
        int reusedCount,
        int newCount,
        int changedCount,
        int scheduledCount) {
}
