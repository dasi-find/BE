package com.dasifind.backend.domain.candidate;

import com.dasifind.backend.domain.candidate.entity.*;
import com.dasifind.backend.domain.candidate.model.*;
import com.dasifind.backend.domain.candidate.repository.CandidateRepository;
import com.dasifind.backend.domain.policeitem.entity.PoliceItem;
import com.dasifind.backend.domain.policeitem.model.*;
import com.dasifind.backend.domain.policeitem.repository.PoliceItemRepository;
import com.dasifind.backend.domain.searchcard.analysis.client.AiAnalysisClientResDTO;
import com.dasifind.backend.domain.searchcard.analysis.entity.SearchCardAnalysis;
import com.dasifind.backend.domain.searchcard.analysis.repository.SearchCardAnalysisRepository;
import com.dasifind.backend.domain.searchcard.entity.SearchCard;
import com.dasifind.backend.domain.searchcard.repository.SearchCardRepository;
import com.dasifind.backend.domain.searchcard.service.SearchCardDeleteService;
import com.dasifind.backend.domain.user.entity.User;
import com.dasifind.backend.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CandidateStorageIntegrationTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 29, 12, 0);
    @Autowired CandidateRepository candidates;
    @Autowired PoliceItemRepository items;
    @Autowired SearchCardRepository cards;
    @Autowired SearchCardAnalysisRepository analyses;
    @Autowired UserRepository users;
    @Autowired SearchCardDeleteService deleteService;
    @Autowired EntityManager em;
    private User owner;
    SearchCard card;

    @BeforeEach
    void setup() {
        owner = users.saveAndFlush(User.create("candidate@example.com", "encoded", "사용자", true));
        card = saveCard();
    }

    @Test
    void 사진과_위치가_없어도_null과_실제_0점_및_근거순서를_보존한다() {
        PoliceItem item = items.saveAndFlush(item(PoliceItemSource.POLICE, "F-1", 1));
        Candidate saved = candidates.saveAndFlush(candidate(card, item));
        em.clear();

        Candidate loaded = candidates.findBySearchCardIdAndPoliceItemId(card.getId(), item.getId()).orElseThrow();
        assertThat(loaded.getId()).isEqualTo(saved.getId());
        assertThat(loaded.getScores().getImageScore()).isNull();
        assertThat(loaded.getScores().getLocationScore()).isNull();
        assertThat(loaded.getScores().getAttributeScore()).isEqualByComparingTo("0");
        assertThat(loaded.getScores().getTextScore()).isEqualByComparingTo("0.8");
        assertThat(loaded.getScores().getImageTextScore()).isEqualByComparingTo("0.6");
        assertThat(loaded.getTotalScore()).isEqualByComparingTo("72.1234");
        assertThat(loaded.getEvidenceCoverage()).isEqualByComparingTo("0.6");
        assertThat(loaded.getEvidence()).extracting(CandidateEvidence::getType)
                .containsExactly(EvidenceType.MISSING, EvidenceType.CONFLICT, EvidenceType.MATCH);
        assertThat(loaded.getScorePolicyVersion()).isEqualTo("policy-test-v1");
        assertThat(loaded.getPoliceItem().getImageUrl()).isNull();
        assertThat(loaded.getPoliceItem().getFoundPlace()).isNull();
        assertThat(loaded.getPoliceItem().getStoragePlace()).isEqualTo("분당경찰서");
    }

    @Test
    void 전부_비교불가이면_최종점수_null과_충족도_0을_보존한다() {
        PoliceItem item = items.saveAndFlush(item(PoliceItemSource.POLICE, "F-1", 1));
        Candidate saved = candidates.saveAndFlush(Candidate.create(card, item,
                CandidateScores.of(null, null, null, null, null, null), null, BigDecimal.ZERO,
                "model-test", "preprocess-test", "policy-test", List.of(), NOW));
        em.clear();
        Candidate loaded = candidates.findById(saved.getId()).orElseThrow();
        assertThat(loaded.getScores().hasComparableScore()).isFalse();
        assertThat(loaded.getTotalScore()).isNull();
        assertThat(loaded.getEvidenceCoverage()).isEqualByComparingTo("0");
    }

    @Test
    void 같은_출처_관리번호_순번은_중복저장할_수_없다() {
        items.saveAndFlush(item(PoliceItemSource.POLICE, "F-1", 1));
        assertThatThrownBy(() -> items.saveAndFlush(item(PoliceItemSource.POLICE, "F-1", 1)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 출처나_순번이_다르면_같은_관리번호여도_보존한다() {
        items.saveAllAndFlush(List.of(item(PoliceItemSource.POLICE, "F-1", 1),
                item(PoliceItemSource.PORTAL, "F-1", 1), item(PoliceItemSource.POLICE, "F-1", 2)));
        assertThat(items.count()).isEqualTo(3);
        assertThat(items.findBySourceAndManagementNoAndItemSequence(PoliceItemSource.PORTAL, "F-1", 1))
                .isPresent();
    }

    @Test
    void 같은_카드와_습득물_쌍은_중복저장할_수_없다() {
        PoliceItem item = items.saveAndFlush(item(PoliceItemSource.POLICE, "F-1", 1));
        candidates.saveAndFlush(candidate(card, item));
        assertThatThrownBy(() -> candidates.saveAndFlush(candidate(card, item)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 재분석은_후보_ID를_유지하고_근거와_버전을_교체한다() {
        PoliceItem item = items.saveAndFlush(item(PoliceItemSource.POLICE, "F-1", 1));
        Candidate saved = candidates.saveAndFlush(candidate(card, item));
        long version = saved.getVersion();
        saved.replaceAssessment(CandidateScores.of(null, BigDecimal.ONE, null, null, null, null),
                new BigDecimal("100"), new BigDecimal("0.2"), "model-v2", "preprocess-v2", "policy-v2",
                List.of(CandidateEvidence.of(EvidenceType.MATCH, ScoreElement.TEXT, "설명 의미가 유사합니다.")),
                NOW.plusMinutes(1));
        candidates.flush();
        em.clear();
        Candidate loaded = candidates.findById(saved.getId()).orElseThrow();
        assertThat(candidates.count()).isEqualTo(1);
        assertThat(loaded.getEvidence()).hasSize(1);
        assertThat(loaded.getScores().getAttributeScore()).isNull();
        assertThat(loaded.getScorePolicyVersion()).isEqualTo("policy-v2");
        assertThat(loaded.getVersion()).isGreaterThan(version);
        assertThat(loaded.getCreatedAt()).isEqualTo(NOW);
        assertThat(loaded.getUpdatedAt()).isEqualTo(NOW.plusMinutes(1));
    }

    @Test
    void 카드_삭제는_후보와_근거만_삭제하고_공유_습득물과_다른카드를_보존한다() {
        PoliceItem item = items.saveAndFlush(item(PoliceItemSource.POLICE, "F-1", 1));
        Candidate removed = candidates.saveAndFlush(candidate(card, item));
        SearchCard otherCard = saveCard();
        Candidate retained = candidates.saveAndFlush(candidate(otherCard, item));
        deleteService.delete(owner.getId(), card.getId());
        em.clear();

        assertThat(candidates.findById(removed.getId())).isEmpty();
        assertThat(candidates.findById(retained.getId())).isPresent();
        assertThat(items.findById(item.getId())).isPresent();
        Number count = (Number) em.createNativeQuery(
                "select count(*) from candidate_evidence where candidate_id = :id")
                .setParameter("id", removed.getId()).getSingleResult();
        assertThat(count.longValue()).isZero();
        assertThat(candidates.findById(retained.getId()).orElseThrow().getEvidence()).hasSize(3);
    }

    @Test
    void 참조된_습득물을_삭제하여_후보를_고아로_만들_수_없다() {
        PoliceItem item = items.saveAndFlush(item(PoliceItemSource.POLICE, "F-1", 1));
        candidates.saveAndFlush(candidate(card, item));
        em.clear();
        assertThatThrownBy(() -> {
            items.deleteById(item.getId());
            items.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 습득물_표시정보_수정시_출처_키를_유지한다() {
        PoliceItem saved = items.saveAndFlush(item(PoliceItemSource.PORTAL, "F-2", 1));
        saved.updateDetails(new PoliceItemDetails("수정된 지갑", "WALLET", "NAVY", "공식 설명",
                null, "판교역", "포털 보관기관", null, null), NOW.plusDays(1));
        items.flush();
        em.clear();
        PoliceItem loaded = items.findById(saved.getId()).orElseThrow();
        assertThat(loaded.getManagementNo()).isEqualTo("F-2");
        assertThat(loaded.getSource()).isEqualTo(PoliceItemSource.PORTAL);
        assertThat(loaded.getItemName()).isEqualTo("수정된 지갑");
        assertThat(loaded.getFoundPlace()).isEqualTo("판교역");
        assertThat(loaded.getVersion()).isPositive();
    }

    private SearchCard saveCard() {
        SearchCardAnalysis analysis = analyses.saveAndFlush(SearchCardAnalysis.create(owner.getId(),
                new AiAnalysisClientResDTO("WALLET", "CARD_WALLET", List.of("BLACK"), null,
                        List.of(), null, List.of("앞면 로고"), "preprocess-test")));
        return cards.saveAndFlush(SearchCard.create(owner.getId(), analysis.getId(),
                "WALLET", "지갑", List.of("BLACK"), null, null, "앞면 로고",
                LocalDate.of(2026, 9, 25), null, null, NOW));
    }

    PoliceItem item(PoliceItemSource source, String no, int sequence) {
        return PoliceItem.create(source, no, sequence, new PoliceItemDetails(
                "검정 반지갑", "OTHER", "BLACK", "공식 등록 설명", LocalDate.of(2026, 9, 26),
                null, "분당경찰서", null, null), NOW);
    }

    Candidate candidate(SearchCard searchCard, PoliceItem item) {
        return Candidate.create(searchCard, item,
                CandidateScores.of(null, new BigDecimal("0.8"), new BigDecimal("0.6"),
                        BigDecimal.ZERO, BigDecimal.ONE, null),
                new BigDecimal("72.1234"), new BigDecimal("0.6"),
                "model-test", "preprocess-test", "policy-test-v1", List.of(
                        CandidateEvidence.of(EvidenceType.MISSING, ScoreElement.IMAGE, "사용자 사진 없음"),
                        CandidateEvidence.of(EvidenceType.CONFLICT, ScoreElement.ATTRIBUTE, "색상 충돌"),
                        CandidateEvidence.of(EvidenceType.MATCH, ScoreElement.DATE, "날짜가 가깝습니다.")), NOW);
    }
}
