package info.isaksson.erland.repofleet.standards;

import info.isaksson.erland.repofleet.repository.api.RepositorySummary;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Clock;
import java.util.List;

@ApplicationScoped
public class RepositoryComplianceEvaluationService {

    private final RepositoryRuleAssignmentService assignments;
    private final RepositoryRuleEvaluator evaluator;
    private final Clock clock;

    @Inject
    public RepositoryComplianceEvaluationService(
            RepositoryRuleAssignmentService assignments,
            RepositoryRuleEvaluator evaluator) {
        this(assignments, evaluator, Clock.systemUTC());
    }

    RepositoryComplianceEvaluationService(
            RepositoryRuleAssignmentService assignments,
            RepositoryRuleEvaluator evaluator,
            Clock clock) {
        this.assignments = assignments;
        this.evaluator = evaluator;
        this.clock = clock;
    }

    public List<RepositoryRuleEvaluation> evaluate(RepositorySummary repository) {
        return assignments.applicableRules(repository).stream()
                .map(applicable -> evaluator.evaluate(
                        repository,
                        applicable.rule(),
                        clock.instant()))
                .toList();
    }
}
