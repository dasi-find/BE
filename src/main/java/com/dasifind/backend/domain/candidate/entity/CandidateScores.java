package com.dasifind.backend.domain.candidate.entity;

import com.dasifind.backend.domain.candidate.model.ScoreElement;
import com.dasifind.backend.domain.candidate.model.ScoreValues;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import java.math.BigDecimal;

/** Calibrated scores, not raw model outputs. Null means unavailable; zero remains zero. */
@Getter
@Embeddable
public class CandidateScores {
    @Column(name = "image_score", precision = 7, scale = 6)
    private BigDecimal imageScore;
    @Column(name = "text_score", precision = 7, scale = 6)
    private BigDecimal textScore;
    @Column(name = "image_text_score", precision = 7, scale = 6)
    private BigDecimal imageTextScore;
    @Column(name = "attribute_score", precision = 7, scale = 6)
    private BigDecimal attributeScore;
    @Column(name = "date_score", precision = 7, scale = 6)
    private BigDecimal dateScore;
    @Column(name = "location_score", precision = 7, scale = 6)
    private BigDecimal locationScore;

    protected CandidateScores() {
    }

    public static CandidateScores of(BigDecimal image, BigDecimal text, BigDecimal imageText,
                                     BigDecimal attribute, BigDecimal date, BigDecimal location) {
        CandidateScores scores = new CandidateScores();
        scores.imageScore = ScoreValues.unit(image);
        scores.textScore = ScoreValues.unit(text);
        scores.imageTextScore = ScoreValues.unit(imageText);
        scores.attributeScore = ScoreValues.unit(attribute);
        scores.dateScore = ScoreValues.unit(date);
        scores.locationScore = ScoreValues.unit(location);
        return scores;
    }

    public BigDecimal valueOf(ScoreElement element) {
        return switch (element) {
            case IMAGE -> imageScore;
            case TEXT -> textScore;
            case IMAGE_TEXT -> imageTextScore;
            case ATTRIBUTE -> attributeScore;
            case DATE -> dateScore;
            case LOCATION -> locationScore;
        };
    }

    public boolean hasComparableScore() {
        for (ScoreElement element : ScoreElement.values()) {
            if (valueOf(element) != null) return true;
        }
        return false;
    }
}
