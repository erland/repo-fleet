package info.isaksson.erland.repofleet.github.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import info.isaksson.erland.repofleet.repository.api.RepositoryVisibility;
import info.isaksson.erland.repofleet.repository.persistence.RepositoryEnrichmentSnapshotRepository;
import info.isaksson.erland.repofleet.repository.persistence.RepositoryIdentity;
import info.isaksson.erland.repofleet.repository.persistence.RepositoryIdentityRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.Set;

@ApplicationScoped
public class GitHubWebhookEventProcessor {

    private static final Set<String> LIFECYCLE_ACTIONS = Set.of(
            "created",
            "edited",
            "renamed",
            "transferred",
            "archived",
            "unarchived",
            "deleted");

    private final ObjectMapper objectMapper;
    private final RepositoryIdentityRepository identities;
    private final RepositoryEnrichmentSnapshotRepository snapshots;

    @Inject
    public GitHubWebhookEventProcessor(
            ObjectMapper objectMapper,
            RepositoryIdentityRepository identities,
            RepositoryEnrichmentSnapshotRepository snapshots) {
        this.objectMapper = objectMapper;
        this.identities = identities;
        this.snapshots = snapshots;
    }

    @Transactional
    public void process(String eventType, String payload, Instant receivedAt) {
        if (!"repository".equals(eventType)) {
            return;
        }

        JsonNode root = read(payload);
        String action = text(root, "action");
        if (!LIFECYCLE_ACTIONS.contains(action)) {
            return;
        }

        JsonNode repository = root.path("repository");
        if (repository.isMissingNode() || repository.isNull()) {
            throw new IllegalArgumentException("Repository webhook payload is missing repository");
        }

        long repositoryId = repository.path("id").asLong(0L);
        if (repositoryId <= 0) {
            return;
        }

        if ("deleted".equals(action)) {
            identities.findByGitHubRepositoryId(repositoryId).ifPresent(identity -> {
                identity.active = false;
                identity.lastSeenAt = receivedAt;
                identity.changeClassification = "LIKELY_CHANGED";
                identity.changeDetectedAt = receivedAt;
            });
            return;
        }

        String fullName = text(repository, "full_name");
        String name = text(repository, "name");
        String owner = text(repository.path("owner"), "login");
        if (fullName.isBlank() || name.isBlank() || owner.isBlank()) {
            throw new IllegalArgumentException("Repository webhook payload is missing identity fields");
        }
        RepositoryVisibility visibility = visibility(repository);
        boolean archived = repository.path("archived").asBoolean(false);
        boolean fork = repository.path("fork").asBoolean(false);
        String defaultBranch = nullableText(repository, "default_branch");
        Instant githubUpdatedAt = instant(repository, "updated_at");
        Instant githubPushedAt = instant(repository, "pushed_at");

        RepositoryIdentity identity = identities.findByGitHubRepositoryId(repositoryId).orElse(null);
        if (identity == null) {
            identity = identities.insert(
                    repositoryId,
                    owner,
                    name,
                    fullName,
                    visibility,
                    archived,
                    fork,
                    defaultBranch,
                    githubUpdatedAt,
                    githubPushedAt,
                    receivedAt);
        } else {
            identities.update(
                    repositoryId,
                    owner,
                    name,
                    fullName,
                    visibility,
                    archived,
                    fork,
                    defaultBranch,
                    githubUpdatedAt,
                    githubPushedAt,
                    receivedAt,
                    true);
        }

        identity.changeClassification = "LIKELY_CHANGED";
        identity.changeDetectedAt = receivedAt;

        snapshots.findByGitHubRepositoryId(repositoryId).ifPresent(snapshot -> {
            snapshot.lastSuccessfulRefreshAt = null;
            snapshot.enrichmentMessage =
                    "Repository metadata changed via GitHub webhook; enrichment refresh pending.";
            snapshot.updatedAt = receivedAt;
        });
    }

    private JsonNode read(String payload) {
        try {
            return objectMapper.readTree(payload);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid GitHub webhook JSON payload", exception);
        }
    }

    private RepositoryVisibility visibility(JsonNode repository) {
        String value = nullableText(repository, "visibility");
        if (value != null) {
            try {
                return RepositoryVisibility.valueOf(value.toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // Fall back to the private flag.
            }
        }
        return repository.path("private").asBoolean(false)
                ? RepositoryVisibility.PRIVATE
                : RepositoryVisibility.PUBLIC;
    }

    private Instant instant(JsonNode node, String field) {
        String value = nullableText(node, field);
        if (value == null) return null;
        try {
            return Instant.parse(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String text(JsonNode node, String field) {
        String value = nullableText(node, field);
        return value == null ? "" : value;
    }

    private String nullableText(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }
}
