package com.dasifind.backend.domain.candidate.entity;

import com.dasifind.backend.domain.candidate.model.ScoreValues;
import com.dasifind.backend.domain.candidate.model.EvidenceType;
import com.dasifind.backend.domain.policeitem.entity.PoliceItem;
import com.dasifind.backend.domain.searchcard.entity.SearchCard;
import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Entity
@Table(name = "candidate", uniqueConstraints = @UniqueConstraint(
        name = "uk_candidate_card_item", columnNames = {"search_card_id", "police_item_id"}))
public class Candidate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "search_card_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private SearchCard searchCard;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "police_item_id", nullable = false)
    private PoliceItem policeItem;
    @Embedded
    private CandidateScores scores;
    @Column(name = "total_score", precision = 7, scale = 4)
    private BigDecimal totalScore;
    @Column(name = "evidence_coverage", nullable = false, precision = 7, scale = 6)
    private BigDecimal evidenceCoverage;
    @Column(name = "model_version", nullable = false, length = 100)
    private String modelVersion;
    @Column(name = "preprocessing_version", nullable = false, length = 100)
    private String preprocessingVersion;
    @Column(name = "score_policy_version", nullable = false, length = 100)
    private String scorePolicyVersion;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    @Version
    private long version;
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "candidate_evidence", joinColumns = @JoinColumn(name = "candidate_id"))
    @OrderColumn(name = "sort_order")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<CandidateEvidence> evidence = new ArrayList<>();

    protected Candidate() {
    }

    public static Candidate create(SearchCard searchCard, PoliceItem policeItem, CandidateScores scores,
                                   BigDecimal totalScore, BigDecimal evidenceCoverage,
                                   String modelVersion, String preprocessingVersion, String scorePolicyVersion,
                                   List<CandidateEvidence> evidence, LocalDateTime now) {
        Candidate candidate = new Candidate();
        candidate.searchCard = Objects.requireNonNull(searchCard, "searchCard");
        candidate.policeItem = Objects.requireNonNull(policeItem, "policeItem");
        candidate.createdAt = Objects.requireNonNull(now, "now");
        candidate.replaceAssessment(scores, totalScore, evidenceCoverage, modelVersion,
                preprocessingVersion, scorePolicyVersion, evidence, now);
        return candidate;
    }

    public void replaceAssessment(CandidateScores scores, BigDecimal totalScore, BigDecimal evidenceCoverage,
                                  String modelVersion, String preprocessingVersion, String scorePolicyVersion,
                                  List<CandidateEvidence> evidence, LocalDateTime now) {
        Objects.requireNonNull(scores, "scores");
        Objects.requireNonNull(evidenceCoverage, "evidenceCoverage");
        Objects.requireNonNull(now, "now");
        ScoreValues.total(totalScore);
        ScoreValues.unit(evidenceCoverage);
        if ((!scores.hasComparableScore() && (totalScore != null || evidenceCoverage.signum() != 0))
                || (totalScore != null && evidenceCoverage.signum() == 0)) {
            throw new IllegalArgumentException("A final score requires comparable evidence and non-zero coverage");
        }
        validateVersion(modelVersion);
        validateVersion(preprocessingVersion);
        validateVersion(scorePolicyVersion);
        List<CandidateEvidence> evidenceCopy = List.copyOf(evidence);
        for (CandidateEvidence reason : evidenceCopy) {
            if (reason.getType() != EvidenceType.MISSING && scores.valueOf(reason.getElement()) == null) {
                throw new IllegalArgumentException("Match/conflict evidence needs a compared score");
            }
        }
        if (now.isBefore(createdAt) || (updatedAt != null && now.isBefore(updatedAt))) {
            throw new IllegalArgumentException("Assessment time cannot move backwards");
        }
        this.scores = scores;
        this.totalScore = totalScore;
        this.evidenceCoverage = evidenceCoverage;
        this.modelVersion = modelVersion;
        this.preprocessingVersion = preprocessingVersion;
        this.scorePolicyVersion = scorePolicyVersion;
        this.evidence.clear();
        this.evidence.addAll(evidenceCopy);
        this.updatedAt = now;
    }

    // Hibernate can materialize an all-null embeddable as null.
    public CandidateScores getScores() {
        return scores == null ? CandidateScores.of(null, null, null, null, null, null) : scores;
    }

    public List<CandidateEvidence> getEvidence() {
        return List.copyOf(evidence);
    }

    private static void validateVersion(String version) {
        if (version == null || version.isBlank() || version.length() > 100) {
            throw new IllegalArgumentException("An explicit version of at most 100 characters is required");
        }
    }
}
