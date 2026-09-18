package info.isaksson.erland.repofleet.repository.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import info.isaksson.erland.repofleet.repository.api.ActivityStatus;
import info.isaksson.erland.repofleet.repository.api.AnalysisState;
import info.isaksson.erland.repofleet.repository.api.GitHubActionsStatus;
import info.isaksson.erland.repofleet.repository.api.LicensePresence;
import info.isaksson.erland.repofleet.repository.api.LicenseStatus;
import info.isaksson.erland.repofleet.repository.api.ReleaseStatus;
import info.isaksson.erland.repofleet.repository.api.RepositoryRefreshStatus;
import info.isaksson.erland.repofleet.repository.api.RepositorySummary;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.List;

@ApplicationScoped
public class RepositoryEnrichmentSnapshotService {

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};

    private final RepositoryEnrichmentSnapshotRepository snapshotRepository;
    private final ObjectMapper objectMapper;

    @Inject
    public RepositoryEnrichmentSnapshotService(
            RepositoryEnrichmentSnapshotRepository snapshotRepository,
            ObjectMapper objectMapper) {
        this.snapshotRepository = snapshotRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public RepositoryEnrichmentSnapshot persistProgressiveResult(
            RepositorySummary summary,
            Instant completedAt) {
        RepositoryRefreshStatus refreshStatus = summary.refreshStatus();
        AnalysisState state = refreshStatus == null
                ? AnalysisState.FAILED
                : refreshStatus.state();

        if (state == AnalysisState.FAILED) {
            return snapshotRepository.findByGitHubRepositoryId(summary.id())
                    .map(snapshot -> {
                        snapshot.enrichmentState = AnalysisState.FAILED.name();
                        snapshot.enrichmentMessage = refreshStatus == null ? null : refreshStatus.message();
                        snapshot.lastRelevantError = refreshStatus == null ? null : refreshStatus.message();
                        snapshot.updatedAt = completedAt;
                        return snapshot;
                    })
                    .orElseGet(() -> saveSnapshot(
                            summary,
                            null,
                            refreshStatus == null ? null : refreshStatus.message(),
                            completedAt));
        }

        RepositoryEnrichmentSnapshot existing =
                snapshotRepository.findByGitHubRepositoryId(summary.id()).orElse(null);
        Instant lastSuccessfulRefreshAt =
                state == AnalysisState.COMPLETE
                        ? completedAt
                        : existing == null ? null : existing.lastSuccessfulRefreshAt;

        return saveSnapshot(summary, lastSuccessfulRefreshAt, null, completedAt);
    }

    @Transactional
    public RepositoryEnrichmentSnapshot saveSnapshot(
            RepositorySummary summary,
            Instant lastSuccessfulRefreshAt,
            String lastRelevantError) {
        return saveSnapshot(summary, lastSuccessfulRefreshAt, lastRelevantError, Instant.now());
    }

    private RepositoryEnrichmentSnapshot saveSnapshot(
            RepositorySummary summary,
            Instant lastSuccessfulRefreshAt,
            String lastRelevantError,
            Instant updatedAt) {
        RepositoryEnrichmentSnapshot snapshot =
                snapshotRepository.findByGitHubRepositoryId(summary.id())
                        .orElseGet(RepositoryEnrichmentSnapshot::new);

        snapshot.githubRepositoryId = summary.id();
        snapshot.topicsJson = writeList(summary.topics());
        snapshot.languagesJson = writeList(summary.languages());
        snapshot.primaryLanguage = summary.primaryLanguage();

        LicenseStatus license = summary.license();
        snapshot.licenseAnalysisState = stateName(
                license == null ? AnalysisState.NOT_ANALYZED : license.analysisState());
        snapshot.licensePresence = (license == null || license.presence() == null)
                ? LicensePresence.UNKNOWN.name()
                : license.presence().name();
        snapshot.licenseRecognized = license == null ? null : license.recognized();
        snapshot.licenseKey = license == null ? null : license.key();
        snapshot.licenseName = license == null ? null : license.name();

        GitHubActionsStatus actions = summary.githubActions();
        snapshot.actionsAnalysisState = stateName(
                actions == null ? AnalysisState.NOT_ANALYZED : actions.analysisState());
        snapshot.workflowsPresent = actions == null ? null : actions.workflowsPresent();
        snapshot.workflowCount = actions == null ? null : actions.workflowCount();

        ReleaseStatus release = summary.release();
        snapshot.releaseAnalysisState = stateName(
                release == null ? AnalysisState.NOT_ANALYZED : release.analysisState());
        snapshot.releasePresent = release == null ? null : release.releasePresent();
        snapshot.latestReleaseName = release == null ? null : release.latestReleaseName();
        snapshot.latestReleaseTag = release == null ? null : release.latestReleaseTag();
        snapshot.latestReleaseDate = release == null ? null : release.latestReleaseDate();
        snapshot.latestReleasePrerelease =
                release == null ? null : release.latestReleasePrerelease();

        ActivityStatus activity = summary.activity();
        snapshot.activityPushedAt = activity == null ? null : activity.pushedAt();
        snapshot.activityUpdatedAt = activity == null ? null : activity.updatedAt();

        RepositoryRefreshStatus refreshStatus = summary.refreshStatus();
        snapshot.enrichmentState = stateName(
                refreshStatus == null ? AnalysisState.NOT_ANALYZED : refreshStatus.state());
        snapshot.enrichmentMessage = refreshStatus == null ? null : refreshStatus.message();
        snapshot.lastSuccessfulRefreshAt = lastSuccessfulRefreshAt;
        snapshot.lastRelevantError = lastRelevantError;
        snapshot.updatedAt = updatedAt;

        if (snapshot.id == null) {
            snapshotRepository.persist(snapshot);
        }
        return snapshot;
    }

    public RepositorySummary reconstruct(RepositoryIdentity identity) {
        return snapshotRepository.findByGitHubRepositoryId(identity.githubRepositoryId)
                .map(snapshot -> reconstruct(identity, snapshot))
                .orElseGet(() -> fallback(identity));
    }

    RepositorySummary reconstruct(
            RepositoryIdentity identity,
            RepositoryEnrichmentSnapshot snapshot) {
        return new RepositorySummary(
                identity.githubRepositoryId,
                identity.ownerLogin,
                identity.name,
                identity.fullName,
                "https://github.com/" + identity.fullName,
                identity.visibility,
                identity.archived,
                identity.fork,
                identity.defaultBranch,
                readList(snapshot.topicsJson),
                readList(snapshot.languagesJson),
                snapshot.primaryLanguage,
                new LicenseStatus(
                        state(snapshot.licenseAnalysisState),
                        presence(snapshot.licensePresence),
                        snapshot.licenseRecognized,
                        snapshot.licenseKey,
                        snapshot.licenseName),
                new GitHubActionsStatus(
                        state(snapshot.actionsAnalysisState),
                        snapshot.workflowsPresent,
                        snapshot.workflowCount),
                new ReleaseStatus(
                        state(snapshot.releaseAnalysisState),
                        snapshot.releasePresent,
                        snapshot.latestReleaseName,
                        snapshot.latestReleaseTag,
                        snapshot.latestReleaseDate,
                        snapshot.latestReleasePrerelease),
                new ActivityStatus(snapshot.activityPushedAt, snapshot.activityUpdatedAt),
                new RepositoryRefreshStatus(
                        state(snapshot.enrichmentState),
                        snapshot.enrichmentMessage));
    }

    private RepositorySummary fallback(RepositoryIdentity identity) {
        return new RepositorySummary(
                identity.githubRepositoryId,
                identity.ownerLogin,
                identity.name,
                identity.fullName,
                "https://github.com/" + identity.fullName,
                identity.visibility,
                identity.archived,
                identity.fork,
                identity.defaultBranch,
                List.of(),
                List.of(),
                null,
                new LicenseStatus(
                        AnalysisState.NOT_ANALYZED,
                        LicensePresence.UNKNOWN,
                        null,
                        null,
                        null),
                new GitHubActionsStatus(AnalysisState.NOT_ANALYZED, null, null),
                new ReleaseStatus(AnalysisState.NOT_ANALYZED, null, null, null, null, null),
                new ActivityStatus(identity.githubPushedAt, identity.githubUpdatedAt),
                new RepositoryRefreshStatus(
                        AnalysisState.NOT_ANALYZED,
                        "Loaded from persisted repository cache; enrichment refresh pending."));
    }

    private String writeList(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : values);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize repository enrichment list.", exception);
        }
    }

    private List<String> readList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return List.copyOf(objectMapper.readValue(json, STRING_LIST));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not deserialize repository enrichment list.", exception);
        }
    }

    private String stateName(AnalysisState state) {
        return (state == null ? AnalysisState.NOT_ANALYZED : state).name();
    }

    private AnalysisState state(String value) {
        return value == null ? AnalysisState.NOT_ANALYZED : AnalysisState.valueOf(value);
    }

    private LicensePresence presence(String value) {
        return value == null ? LicensePresence.UNKNOWN : LicensePresence.valueOf(value);
    }
}
