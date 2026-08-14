package info.isaksson.erland.repofleet.repository.inventory;

import info.isaksson.erland.repofleet.repository.api.RepositorySummary;

public interface RepositoryEnrichmentService {
    RepositorySummary enrich(RepositorySummary repository);
}
