package info.isaksson.erland.repofleet.standards;

import java.util.List;

public record ApplicableRepositoryRule(
        RepositoryStandardRuleDefinition rule,
        ApplicationReason reason,
        List<String> matchingGroups) {

    public ApplicableRepositoryRule {
        matchingGroups = matchingGroups == null ? List.of() : List.copyOf(matchingGroups);
    }

    public enum ApplicationReason {
        ALL_REPOSITORIES,
        MATCHING_GROUP
    }
}
