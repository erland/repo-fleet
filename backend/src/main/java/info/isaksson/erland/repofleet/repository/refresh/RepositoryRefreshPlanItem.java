package info.isaksson.erland.repofleet.repository.refresh;

import info.isaksson.erland.repofleet.repository.api.RepositorySummary;

public record RepositoryRefreshPlanItem(
        RepositorySummary discovered,
        RepositoryRefreshAction action,
        RepositorySummary cached) {
}
