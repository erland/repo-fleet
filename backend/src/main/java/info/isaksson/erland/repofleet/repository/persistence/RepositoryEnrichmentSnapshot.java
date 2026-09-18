package info.isaksson.erland.repofleet.repository.persistence;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "repository_enrichment_snapshot")
public class RepositoryEnrichmentSnapshot extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "github_repository_id", nullable = false, unique = true)
    public long githubRepositoryId;

    @Column(name = "topics_json", nullable = false, columnDefinition = "text")
    public String topicsJson;

    @Column(name = "languages_json", nullable = false, columnDefinition = "text")
    public String languagesJson;

    @Column(name = "primary_language")
    public String primaryLanguage;

    @Column(name = "license_analysis_state", nullable = false)
    public String licenseAnalysisState;

    @Column(name = "license_presence", nullable = false)
    public String licensePresence;

    @Column(name = "license_recognized")
    public Boolean licenseRecognized;

    @Column(name = "license_key")
    public String licenseKey;

    @Column(name = "license_name")
    public String licenseName;

    @Column(name = "actions_analysis_state", nullable = false)
    public String actionsAnalysisState;

    @Column(name = "workflows_present")
    public Boolean workflowsPresent;

    @Column(name = "workflow_count")
    public Integer workflowCount;

    @Column(name = "release_analysis_state", nullable = false)
    public String releaseAnalysisState;

    @Column(name = "release_present")
    public Boolean releasePresent;

    @Column(name = "latest_release_name")
    public String latestReleaseName;

    @Column(name = "latest_release_tag")
    public String latestReleaseTag;

    @Column(name = "latest_release_date")
    public Instant latestReleaseDate;

    @Column(name = "latest_release_prerelease")
    public Boolean latestReleasePrerelease;

    @Column(name = "activity_pushed_at")
    public Instant activityPushedAt;

    @Column(name = "activity_updated_at")
    public Instant activityUpdatedAt;

    @Column(name = "enrichment_state", nullable = false)
    public String enrichmentState;

    @Column(name = "enrichment_message", columnDefinition = "text")
    public String enrichmentMessage;

    @Column(name = "last_successful_refresh_at")
    public Instant lastSuccessfulRefreshAt;

    @Column(name = "last_relevant_error", columnDefinition = "text")
    public String lastRelevantError;

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;
}
