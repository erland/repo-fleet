package info.isaksson.erland.repofleet.repository.api;

public record GitHubActionsStatus(
    AnalysisState analysisState,
    Boolean workflowsPresent,
    Integer workflowCount
) {
}
