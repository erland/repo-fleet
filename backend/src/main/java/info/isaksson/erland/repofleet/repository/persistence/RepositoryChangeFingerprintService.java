package info.isaksson.erland.repofleet.repository.persistence;

import info.isaksson.erland.repofleet.repository.api.RepositorySummary;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Objects;

@ApplicationScoped
public class RepositoryChangeFingerprintService {

    public RepositoryChangeClassification classify(
            RepositoryIdentity existing,
            RepositorySummary discovered) {
        if (existing == null) {
            return RepositoryChangeClassification.NEW;
        }

        boolean changed =
                !Objects.equals(existing.ownerLogin, discovered.owner())
                        || !Objects.equals(existing.name, discovered.name())
                        || !Objects.equals(existing.fullName, discovered.fullName())
                        || existing.visibility != discovered.visibility()
                        || existing.archived != discovered.archived()
                        || existing.fork != discovered.fork()
                        || !Objects.equals(existing.defaultBranch, discovered.defaultBranch())
                        || !Objects.equals(
                                existing.githubUpdatedAt,
                                discovered.activity() == null ? null : discovered.activity().updatedAt())
                        || !Objects.equals(
                                existing.githubPushedAt,
                                discovered.activity() == null ? null : discovered.activity().pushedAt());

        return changed
                ? RepositoryChangeClassification.LIKELY_CHANGED
                : RepositoryChangeClassification.APPARENTLY_UNCHANGED;
    }
}
