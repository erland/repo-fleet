package info.isaksson.erland.repofleet.standards;

import info.isaksson.erland.repofleet.repository.api.RepositorySummary;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.List;

@ApplicationScoped
public class RepositoryComplianceService {

    private final RepositoryRuleAssignmentService assignments;
    private final RepositoryRuleEvaluationEngine evaluationEngine;

    @Inject
    public RepositoryComplianceService(
            RepositoryRuleAssignmentService assignments,
            RepositoryRuleEvaluationEngine evaluationEngine) {
        this.assignments = assignments;
        this.evaluationEngine = evaluationEngine;
    }

    public List<RepositoryRuleEvaluation> evaluate(
            RepositorySummary repository,
            Instant now) {
        return assignments.applicableRules(repository).stream()
                .map(applicable -> evaluationEngine.evaluate(repository, applicable.rule(), now))
                .toList();
    }
}
