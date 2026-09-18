package info.isaksson.erland.repofleet.repository.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;

@ApplicationScoped
public class RepositoryEnrichmentSnapshotRepository
        implements PanacheRepository<RepositoryEnrichmentSnapshot> {

    public Optional<RepositoryEnrichmentSnapshot> findByGitHubRepositoryId(long githubRepositoryId) {
        return find("githubRepositoryId", githubRepositoryId).firstResultOptional();
    }
}
