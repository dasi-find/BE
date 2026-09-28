package com.dasifind.backend.domain.candidate;

import com.dasifind.backend.domain.candidate.entity.*;
import com.dasifind.backend.domain.candidate.model.*;
import com.dasifind.backend.domain.policeitem.entity.PoliceItem;
import com.dasifind.backend.domain.policeitem.model.*;
import com.dasifind.backend.domain.searchcard.entity.SearchCard;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class CandidateValidationTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 29, 12, 0);

    @ParameterizedTest
    @ValueSource(strings = {"-0.1", "1.000001", "0.1234567"})
    void 항목별_범위와_정밀도를_검증한다(String value) {
        for (int i = 0; i < 6; i++) {
            BigDecimal[] scores = new BigDecimal[6];
            scores[i] = new BigDecimal(value);
            assertThatThrownBy(() -> CandidateScores.of(
                    scores[0], scores[1], scores[2], scores[3], scores[4], scores[5]))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1", "100.0001", "80.12345"})
    void 최종점수_범위와_정밀도를_검증한다(String value) {
        assertThatThrownBy(() -> ScoreValues.total(new BigDecimal(value)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 근거가_없는_최종점수는_허용하지_않는다() {
        assertThatThrownBy(() -> create(CandidateScores.of(null, null, null, null, null, null),
                BigDecimal.TEN, BigDecimal.ZERO)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> create(CandidateScores.of(null, BigDecimal.ONE, null, null, null, null),
                BigDecimal.TEN, BigDecimal.ZERO)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 실제_0점은_비교한_결과로_유지한다() {
        Candidate candidate = create(CandidateScores.of(null, BigDecimal.ZERO, null, null, null, null),
                BigDecimal.ZERO, new BigDecimal("0.2"));
        assertThat(candidate.getScores().hasComparableScore()).isTrue();
        assertThat(candidate.getTotalScore()).isEqualByComparingTo("0");
    }

    @Test
    void 근거_목록을_외부에서_변경할_수_없다() {
        Candidate candidate = create(CandidateScores.of(null, BigDecimal.ONE, null, null, null, null),
                BigDecimal.TEN, new BigDecimal("0.2"));
        assertThatThrownBy(() -> candidate.getEvidence().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void 잘못된_근거와_출처키를_거부한다() {
        assertThatThrownBy(() -> CandidateEvidence.of(EvidenceType.MATCH, ScoreElement.IMAGE, " "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PoliceItem.create(PoliceItemSource.POLICE, " F1 ", 1, details(), NOW))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PoliceItem.create(PoliceItemSource.POLICE, "F1", 0, details(), NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 비교하지_않은_항목의_일치근거는_거부하고_기존_평가를_보존한다() {
        CandidateScores scores = CandidateScores.of(null, BigDecimal.ONE, null, null, null, null);
        Candidate candidate = create(scores, BigDecimal.TEN, new BigDecimal("0.2"));
        List<CandidateEvidence> previousEvidence = candidate.getEvidence();

        assertThatThrownBy(() -> candidate.replaceAssessment(scores, new BigDecimal("90"),
                new BigDecimal("0.3"), "new-model", "new-preprocess", "new-policy",
                List.of(CandidateEvidence.of(EvidenceType.MATCH, ScoreElement.IMAGE, "사진 일치")),
                NOW.plusMinutes(1))).isInstanceOf(IllegalArgumentException.class);

        assertThat(candidate.getTotalScore()).isEqualByComparingTo("10");
        assertThat(candidate.getModelVersion()).isEqualTo("model");
        assertThat(candidate.getUpdatedAt()).isEqualTo(NOW);
        assertThat(candidate.getEvidence()).isEqualTo(previousEvidence);
    }

    private Candidate create(CandidateScores scores, BigDecimal total, BigDecimal coverage) {
        SearchCard card = SearchCard.create(1L, 1L, "WALLET", "지갑", List.of("BLACK"), null,
                null, "로고", LocalDate.of(2026, 9, 28), null, null, NOW);
        PoliceItem item = PoliceItem.create(PoliceItemSource.POLICE, "F1", 1, details(), NOW);
        return Candidate.create(card, item, scores, total, coverage, "model", "preprocess", "policy",
                List.of(CandidateEvidence.of(EvidenceType.MISSING, ScoreElement.IMAGE, "사진 없음")), NOW);
    }

    private PoliceItemDetails details() {
        return new PoliceItemDetails("지갑", null, null, null, null, null, null, null, null);
    }
}
