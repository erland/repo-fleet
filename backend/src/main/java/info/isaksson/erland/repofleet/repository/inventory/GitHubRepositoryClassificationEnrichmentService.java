package info.isaksson.erland.repofleet.repository.inventory;

import info.isaksson.erland.repofleet.github.api.GitHubApiCallExecutor;
import info.isaksson.erland.repofleet.github.auth.GitHubInstallationTokenService;
import info.isaksson.erland.repofleet.github.client.GitHubRepositoryMetadataClient;
import info.isaksson.erland.repofleet.repository.api.AnalysisState;
import info.isaksson.erland.repofleet.repository.api.CacheFreshness;
import info.isaksson.erland.repofleet.repository.api.GitHubActionsStatus;
import info.isaksson.erland.repofleet.repository.api.LicenseStatus;
import info.isaksson.erland.repofleet.repository.api.ReleaseStatus;
import info.isaksson.erland.repofleet.repository.api.RepositoryRefreshOutcome;
import info.isaksson.erland.repofleet.repository.api.RepositoryRefreshStatus;
import info.isaksson.erland.repofleet.repository.api.RepositorySummary;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class GitHubRepositoryClassificationEnrichmentService implements RepositoryEnrichmentService {

    private final GitHubTopicsEnrichmentComponent topicsEnrichment;
    private final GitHubLanguagesEnrichmentComponent languagesEnrichment;
    private final GitHubLicenseEnrichmentComponent licenseEnrichment;
    private final GitHubActionsEnrichmentComponent actionsEnrichment;
    private final GitHubReleaseEnrichmentComponent releaseEnrichment;

    @Inject
    public GitHubRepositoryClassificationEnrichmentService(
            GitHubTopicsEnrichmentComponent topicsEnrichment,
            GitHubLanguagesEnrichmentComponent languagesEnrichment,
            GitHubLicenseEnrichmentComponent licenseEnrichment,
            GitHubActionsEnrichmentComponent actionsEnrichment,
            GitHubReleaseEnrichmentComponent releaseEnrichment) {
        this.topicsEnrichment = topicsEnrichment;
        this.languagesEnrichment = languagesEnrichment;
        this.licenseEnrichment = licenseEnrichment;
        this.actionsEnrichment = actionsEnrichment;
        this.releaseEnrichment = releaseEnrichment;
    }

    GitHubRepositoryClassificationEnrichmentService(
            GitHubInstallationTokenService tokenService,
            GitHubRepositoryMetadataClient client) {
        this(
                new GitHubTopicsEnrichmentComponent(
                        client,
                        new GitHubApiCallExecutor(tokenService)),
                new GitHubLanguagesEnrichmentComponent(
                        client,
                        new GitHubApiCallExecutor(tokenService)),
                new GitHubLicenseEnrichmentComponent(
                        client,
                        new GitHubApiCallExecutor(tokenService)),
                new GitHubActionsEnrichmentComponent(
                        client,
                        new GitHubApiCallExecutor(tokenService)),
                new GitHubReleaseEnrichmentComponent(
                        client,
                        new GitHubApiCallExecutor(tokenService)));
    }

    @Override
    public RepositorySummary enrich(RepositorySummary repository) {
        List<String> topics = repository.topics();
        List<String> languages = repository.languages();
        String primaryLanguage = repository.primaryLanguage();
        LicenseStatus license = repository.license();
        GitHubActionsStatus githubActions = repository.githubActions();
        ReleaseStatus release = repository.release();
        boolean cachedEnrichmentComplete = repository.refreshStatus() != null
                && repository.refreshStatus().state() == AnalysisState.COMPLETE;
        boolean cachedLicenseComplete = license != null
                && license.analysisState() == AnalysisState.COMPLETE;
        boolean cachedActionsComplete = githubActions != null
                && githubActions.analysisState() == AnalysisState.COMPLETE;
        boolean cachedReleaseComplete = release != null
                && release.analysisState() == AnalysisState.COMPLETE;

        boolean topicsComplete = false;
        boolean languagesComplete = false;
        boolean licenseComplete = false;
        boolean actionsComplete = false;
        boolean releaseComplete = false;
        List<String> errors = new ArrayList<>();

        RepositoryMetadataResult<List<String>> topicsResult =
                topicsEnrichment.enrich(repository, cachedEnrichmentComplete, true);
        if (topicsResult.unavailable()) {
            return unavailableRepository(repository, topicsResult.error());
        }
        topics = topicsResult.value();
        topicsComplete = topicsResult.complete();
        if (topicsResult.degraded() && topicsResult.error() != null) {
            errors.add(topicsResult.error());
        }

        RepositoryMetadataResult<RepositoryLanguageMetadata> languagesResult =
                languagesEnrichment.enrich(repository, cachedEnrichmentComplete, true);
        RepositoryLanguageMetadata languageMetadata = languagesResult.value();
        languages = languageMetadata.languages();
        primaryLanguage = languageMetadata.primaryLanguage();
        languagesComplete = languagesResult.complete();
        if (languagesResult.degraded() && languagesResult.error() != null) {
            errors.add(languagesResult.error());
        }


        RepositoryMetadataResult<LicenseStatus> licenseResult =
                licenseEnrichment.enrich(repository, cachedLicenseComplete);
        license = licenseResult.value();
        licenseComplete = licenseResult.complete();
        if (licenseResult.degraded() && licenseResult.error() != null) {
            errors.add(licenseResult.error());
        }

        RepositoryMetadataResult<GitHubActionsStatus> actionsResult =
                actionsEnrichment.enrich(repository, cachedActionsComplete);
        githubActions = actionsResult.value();
        actionsComplete = actionsResult.complete();
        if (actionsResult.degraded() && actionsResult.error() != null) {
            errors.add(actionsResult.error());
        }

        RepositoryMetadataResult<ReleaseStatus> releaseResult =
                releaseEnrichment.enrich(repository, cachedReleaseComplete, true);
        release = releaseResult.value();
        releaseComplete = releaseResult.complete();
        if (releaseResult.degraded() && releaseResult.error() != null) {
            errors.add(releaseResult.error());
        }

        AnalysisState state;
        String message;
        int completedAnalyses = (topicsComplete ? 1 : 0)
                + (languagesComplete ? 1 : 0)
                + (licenseComplete ? 1 : 0)
                + (actionsComplete ? 1 : 0)
                + (releaseComplete ? 1 : 0);
        if (completedAnalyses == 5) {
            state = AnalysisState.COMPLETE;
            message = "Repository enrichment complete.";
        } else if (completedAnalyses > 0) {
            state = AnalysisState.PARTIAL;
            message = "Repository enrichment partially completed (" + String.join("; ", errors) + ").";
        } else {
            state = AnalysisState.FAILED;
            message = "Repository enrichment failed (" + String.join("; ", errors) + ").";
        }

        RepositoryRefreshOutcome latestOutcome = state == AnalysisState.FAILED
                ? RepositoryRefreshOutcome.FAILED
                : errors.isEmpty()
                        ? RepositoryRefreshOutcome.SUCCESS
                        : RepositoryRefreshOutcome.DEGRADED;
        CacheFreshness freshness = latestOutcome == RepositoryRefreshOutcome.SUCCESS
                ? CacheFreshness.FRESH
                : CacheFreshness.STALE;

        return new RepositorySummary(
                repository.id(),
                repository.owner(),
                repository.name(),
                repository.fullName(),
                repository.url(),
                repository.visibility(),
                repository.archived(),
                repository.fork(),
                repository.defaultBranch(),
                topics,
                languages,
                primaryLanguage,
                license,
                githubActions,
                release,
                repository.activity(),
                new RepositoryRefreshStatus(state, message, freshness, latestOutcome));
    }

    @Override
    public RepositorySummary verifyVolatileMetadata(RepositorySummary repository) {
        List<String> topics = repository.topics();
        ReleaseStatus release = repository.release();
        List<String> errors = new ArrayList<>();
        boolean topicsComplete = false;
        boolean releaseComplete = false;

        RepositoryMetadataResult<List<String>> topicsResult =
                topicsEnrichment.enrich(repository, false, false);
        if (topicsResult.unavailable()) {
            return unavailableRepository(repository, topicsResult.error());
        }
        topics = topicsResult.value();
        topicsComplete = topicsResult.complete();
        if (topicsResult.degraded() && topicsResult.error() != null) {
            errors.add(topicsResult.error());
        }

        boolean cachedReleaseComplete = release != null
                && release.analysisState() == AnalysisState.COMPLETE;
        RepositoryMetadataResult<ReleaseStatus> releaseResult =
                releaseEnrichment.enrich(repository, cachedReleaseComplete, false);
        release = releaseResult.value();
        releaseComplete = releaseResult.complete();
        if (releaseResult.degraded() && releaseResult.error() != null) {
            errors.add(releaseResult.error());
        }

        AnalysisState state = topicsComplete && releaseComplete
                ? AnalysisState.COMPLETE
                : AnalysisState.PARTIAL;
        String message = state == AnalysisState.COMPLETE
                ? "Repository volatile metadata verified."
                : "Repository volatile metadata verification partially completed ("
                        + String.join("; ", errors) + ").";

        RepositoryRefreshOutcome latestOutcome = errors.isEmpty()
                ? RepositoryRefreshOutcome.SUCCESS
                : RepositoryRefreshOutcome.DEGRADED;
        CacheFreshness freshness = latestOutcome == RepositoryRefreshOutcome.SUCCESS
                ? CacheFreshness.FRESH
                : CacheFreshness.STALE;

        return new RepositorySummary(
                repository.id(),
                repository.owner(),
                repository.name(),
                repository.fullName(),
                repository.url(),
                repository.visibility(),
                repository.archived(),
                repository.fork(),
                repository.defaultBranch(),
                topics,
                repository.languages(),
                repository.primaryLanguage(),
                repository.license(),
                repository.githubActions(),
                release,
                repository.activity(),
                new RepositoryRefreshStatus(state, message, freshness, latestOutcome));
    }

    private RepositorySummary unavailableRepository(
            RepositorySummary repository,
            String error) {
        return new RepositorySummary(
                repository.id(),
                repository.owner(),
                repository.name(),
                repository.fullName(),
                repository.url(),
                repository.visibility(),
                repository.archived(),
                repository.fork(),
                repository.defaultBranch(),
                repository.topics(),
                repository.languages(),
                repository.primaryLanguage(),
                repository.license(),
                repository.githubActions(),
                repository.release(),
                repository.activity(),
                new RepositoryRefreshStatus(
                        AnalysisState.FAILED,
                        "Repository became unavailable during refresh: " + error,
                        CacheFreshness.STALE,
                        RepositoryRefreshOutcome.FAILED));
    }

}
