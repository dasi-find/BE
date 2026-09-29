package com.dasifind.backend.domain.candidate.entity;

import com.dasifind.backend.domain.candidate.model.EvidenceType;
import com.dasifind.backend.domain.candidate.model.ScoreElement;
import jakarta.persistence.*;
import lombok.Getter;
import java.util.Objects;

@Getter
@Embeddable
public class CandidateEvidence {
    @Enumerated(EnumType.STRING)
    @Column(name = "evidence_type", nullable = false, length = 20)
    private EvidenceType type;
    @Enumerated(EnumType.STRING)
    @Column(name = "score_element", nullable = false, length = 20)
    private ScoreElement element;
    @Column(nullable = false, length = 1000)
    private String message;

    protected CandidateEvidence() {
    }

    public static CandidateEvidence of(EvidenceType type, ScoreElement element, String message) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(element, "element");
        if (message == null || message.isBlank() || message.length() > 1000) {
            throw new IllegalArgumentException("Evidence message must contain 1 to 1000 characters");
        }
        CandidateEvidence evidence = new CandidateEvidence();
        evidence.type = type;
        evidence.element = element;
        evidence.message = message;
        return evidence;
    }
}
