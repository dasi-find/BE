package com.dasifind.backend.domain.candidate;

import com.dasifind.backend.domain.candidate.entity.Candidate;
import com.dasifind.backend.domain.candidate.entity.CandidateScores;
import com.dasifind.backend.domain.candidate.entity.CandidateEvidence;
import com.dasifind.backend.domain.candidate.model.CandidateFeedback;
import com.dasifind.backend.domain.candidate.model.EvidenceType;
import com.dasifind.backend.domain.candidate.model.ScoreElement;
import com.dasifind.backend.domain.candidate.repository.CandidateRepository;
import com.dasifind.backend.domain.candidate.service.CandidateQueryService;
import com.dasifind.backend.domain.candidate.service.CandidateInteractionService;
import com.dasifind.backend.domain.policeitem.entity.PoliceItem;
import com.dasifind.backend.domain.policeitem.model.PoliceItemDetails;
import com.dasifind.backend.domain.policeitem.model.PoliceItemSource;
import com.dasifind.backend.domain.policeitem.repository.PoliceItemRepository;
import com.dasifind.backend.domain.searchcard.analysis.client.AiAnalysisClientResDTO;
import com.dasifind.backend.domain.searchcard.analysis.entity.SearchCardAnalysis;
import com.dasifind.backend.domain.searchcard.analysis.repository.SearchCardAnalysisRepository;
import com.dasifind.backend.domain.searchcard.entity.SearchCard;
import com.dasifind.backend.domain.searchcard.model.SearchCardStatus;
import com.dasifind.backend.domain.searchcard.model.SearchCardCloseReason;
import com.dasifind.backend.domain.searchcard.repository.SearchCardRepository;
import com.dasifind.backend.domain.user.entity.User;
import com.dasifind.backend.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CandidateApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired CandidateRepository candidates;
    @Autowired PoliceItemRepository items;
    @Autowired SearchCardRepository cards;
    @Autowired SearchCardAnalysisRepository analyses;
    @Autowired UserRepository users;
    @Autowired CandidateQueryService queries;
    @Autowired CandidateInteractionService interactions;
    @Autowired EntityManager em;
    User owner;
    User stranger;
    SearchCard card;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 29, 12, 0);

    @BeforeEach
    void setup() {
        owner = users.saveAndFlush(User.create(UUID.randomUUID() + "@example.com", "encoded", "소유자", true));
        stranger = users.saveAndFlush(User.create(UUID.randomUUID() + "@example.com", "encoded", "다른 사용자", true));
        card = cards.saveAndFlush(SearchCard.create(owner.getId(), analysisId(), "WALLET", "지갑",
                List.of("BLACK"), null, null, "로고", LocalDate.of(2026, 9, 25), null, null, NOW));
    }

    @Test
    void 기본_5개_페이지와_안정적인_순위를_제공한다() throws Exception {
        for (int i = 0; i < 7; i++) save(Integer.toString(90 - i), LocalDate.of(2026, 9, 26));
        em.clear();
        mvc.perform(auth(get(listUrl())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result.size").value(5))
                .andExpect(jsonPath("$.result.content.length()").value(5))
                .andExpect(jsonPath("$.result.totalElements").value(7))
                .andExpect(jsonPath("$.result.hasNext").value(true))
                .andExpect(jsonPath("$.result.content[0].rank").value(1))
                .andExpect(jsonPath("$.result.content[0].isNew").value(true));
        mvc.perform(auth(get(listUrl()).param("page", "1")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result.content[0].rank").value(6))
                .andExpect(jsonPath("$.result.content.length()").value(2))
                .andExpect(jsonPath("$.result.hasNext").value(false));
        mvc.perform(auth(get(listUrl()).param("page", "5")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result.content.length()").value(0))
                .andExpect(jsonPath("$.result.totalElements").value(7));
    }

    @Test
    void 동점은_날짜_null마지막_ID순으로_목록과_상세가_동일하다() {
        Candidate noDate = save("80", null);
        Candidate older = save("80", LocalDate.of(2026, 9, 25));
        Candidate newest1 = save("80", LocalDate.of(2026, 9, 26));
        Candidate newest2 = save("80", LocalDate.of(2026, 9, 26));
        em.clear();
        var result = queries.list(owner.getId(), card.getId(), null, false, 0, 5);
        assertThat(result.content()).extracting(x -> x.candidateId())
                .containsExactly(newest1.getId(), newest2.getId(), older.getId(), noDate.getId());
        for (int i = 0; i < result.content().size(); i++) {
            assertThat(result.content().get(i).rank()).isEqualTo(i + 1L);
            assertThat(queries.detail(owner.getId(), result.content().get(i).candidateId()).rank()).isEqualTo(i + 1L);
        }
    }

    @Test
    void 제외_포함_페이지에서도_제외후보는_순위를_차지하지_않는다() {
        Candidate excluded = save("99", null);
        excluded.updateFeedback(CandidateFeedback.NOT_MINE);
        Candidate first = save("90", null);
        Candidate excluded2 = save("85", null);
        excluded2.updateFeedback(CandidateFeedback.NOT_MINE);
        Candidate second = save("80", null);
        candidates.flush();
        em.clear();
        var page = queries.list(owner.getId(), card.getId(), null, true, 1, 2);
        assertThat(page.content()).extracting(x -> x.candidateId()).containsExactly(excluded2.getId(), second.getId());
        assertThat(page.content().get(0).rank()).isNull();
        assertThat(page.content().get(1).rank()).isEqualTo(2);
        assertThat(queries.detail(owner.getId(), first.getId()).rank()).isEqualTo(1);
        var filtered = queries.list(owner.getId(), card.getId(), new BigDecimal("85"), false, 0, 5);
        assertThat(filtered.content()).extracting(x -> x.candidateId()).containsExactly(first.getId());
        assertThat(filtered.content().get(0).rank()).isEqualTo(1);
    }

    @Test
    void null점수는_목록제외_0점은_유지하고_상세에서_null을_보존한다() throws Exception {
        Candidate missing = save(null, null);
        Candidate zero = save("0", null);
        mvc.perform(auth(get(listUrl())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result.totalElements").value(1))
                .andExpect(jsonPath("$.result.content[0].candidateId").value(zero.getId()))
                .andExpect(jsonPath("$.result.content[0].totalScore").value(0));
        mvc.perform(auth(get(detailUrl(missing))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result.rank").value(nullValue()))
                .andExpect(jsonPath("$.result.totalScore").value(nullValue()))
                .andExpect(jsonPath("$.result.scores.imageScore").value(nullValue()));
    }

    @Test
    void 상세는_DTO와_출처_근거를_제공하며_읽음으로_바꾸지_않는다() throws Exception {
        Candidate saved = save("82", LocalDate.of(2026, 9, 26));
        mvc.perform(auth(get(detailUrl(saved))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("COMMON2001"))
                .andExpect(jsonPath("$.result.policeItem.source").value("POLICE"))
                .andExpect(jsonPath("$.result.policeItem.itemSequence").value(1))
                .andExpect(jsonPath("$.result.scores.textScore").value(0.8))
                .andExpect(jsonPath("$.result.scores.attributeScore").value(0))
                .andExpect(jsonPath("$.result.scores.imageScore").value(nullValue()))
                .andExpect(jsonPath("$.result.evidenceDetails[0].type").value("MISSING"))
                .andExpect(jsonPath("$.result.reasons[0]").value("사진 없음"))
                .andExpect(jsonPath("$.result.isExcluded").value(false))
                .andExpect(jsonPath("$.result.notificationSuppressed").doesNotExist())
                .andExpect(jsonPath("$.result.version").doesNotExist());
        assertThat(saved.getViewedAt()).isNull();
    }

    @Test
    void 읽음은_최초_시간을_유지하고_미확인_표시만_해제한다() throws Exception {
        Candidate saved = save("82", null);
        var first = interactions.view(owner.getId(), saved.getId());
        candidates.flush();
        em.clear();
        assertThat(interactions.view(owner.getId(), saved.getId()).viewedAt()).isEqualTo(first.viewedAt());
        mvc.perform(auth(post(detailUrl(saved) + "/view").contentType(MediaType.APPLICATION_JSON).content("{}")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result.viewedAt").exists());
        mvc.perform(auth(get(listUrl()))).andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content[0].isNew").value(false))
                .andExpect(jsonPath("$.result.content[0].feedback").value(nullValue()));
    }

    @Test
    void 피드백_제외와_복원은_읽음과_독립적이고_알림금지를_보존한다() throws Exception {
        Candidate saved = save("82", null);
        mvc.perform(auth(put(detailUrl(saved) + "/feedback").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"feedback\":\"NOT_MINE\"}")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result.isExcluded").value(true));
        candidates.flush();
        em.clear();
        assertThat(candidates.findById(saved.getId()).orElseThrow().isNotificationSuppressed()).isTrue();
        mvc.perform(auth(get(listUrl()))).andExpect(jsonPath("$.result.content.length()").value(0));
        mvc.perform(auth(get(listUrl()).param("includeExcluded", "true")))
                .andExpect(jsonPath("$.result.content[0].feedback").value("NOT_MINE"))
                .andExpect(jsonPath("$.result.content[0].rank").value(nullValue()));
        mvc.perform(auth(delete(detailUrl(saved) + "/feedback")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result.feedback").value(nullValue()))
                .andExpect(jsonPath("$.result.isExcluded").value(false));
        mvc.perform(auth(delete(detailUrl(saved) + "/feedback"))).andExpect(status().isOk());
        candidates.flush();
        em.clear();
        Candidate restored = candidates.findById(saved.getId()).orElseThrow();
        assertThat(restored.isNotificationSuppressed()).isTrue();
        assertThat(restored.getViewedAt()).isNull();
        assertThat(queries.list(owner.getId(), card.getId(), null, false, 0, 5).content()).hasSize(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"VERY_SIMILAR", "UNSURE"})
    void 유효한_피드백을_수정해도_과거_알림금지는_유지한다(String feedback) throws Exception {
        Candidate saved = save("82", null);
        saved.updateFeedback(CandidateFeedback.NOT_MINE);
        mvc.perform(auth(put(detailUrl(saved) + "/feedback").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"feedback\":\"" + feedback + "\"}")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result.feedback").value(feedback))
                .andExpect(jsonPath("$.result.isExcluded").value(false));
        assertThat(saved.isNotificationSuppressed()).isTrue();
    }

    @Test
    void 재평가가_읽음_피드백_알림금지를_초기화하지_않는다() {
        Candidate saved = save("82", null);
        saved.markViewed(NOW);
        saved.updateFeedback(CandidateFeedback.NOT_MINE);
        saved.replaceAssessment(saved.getScores(), BigDecimal.TEN, saved.getEvidenceCoverage(),
                "m2", "p2", "s2", saved.getEvidence(), NOW.plusMinutes(1));
        candidates.flush();
        em.clear();
        var loaded = candidates.findById(saved.getId()).orElseThrow();
        assertThat(loaded.getViewedAt()).isEqualTo(NOW);
        assertThat(loaded.getFeedback()).isEqualTo(CandidateFeedback.NOT_MINE);
        assertThat(loaded.isNotificationSuppressed()).isTrue();
        assertThat(loaded.isCurrentAssessment()).isTrue();
    }

    @Test
    void 카드_수정전_평가는_숨기고_재평가하면_복원한다() throws Exception {
        Candidate saved = save("82", null);
        card.update(analysisId(), "WALLET", "변경된 지갑", List.of("NAVY"), null, null, "다른 특징",
                LocalDate.of(2026, 9, 25), null, null, NOW.plusMinutes(1));
        cards.flush();
        mvc.perform(auth(get(listUrl()))).andExpect(jsonPath("$.result.totalElements").value(0));
        mvc.perform(auth(get(detailUrl(saved)))).andExpect(status().isNotFound());
        mvc.perform(auth(post(detailUrl(saved) + "/view"))).andExpect(status().isNotFound());
        saved.replaceAssessment(saved.getScores(), BigDecimal.TEN, saved.getEvidenceCoverage(),
                "m2", "p2", "s2", saved.getEvidence(), NOW.plusMinutes(2));
        candidates.flush();
        mvc.perform(auth(get(detailUrl(saved)))).andExpect(status().isOk());
    }

    @Test
    void 습득물_갱신전_평가와_버전없는_기존평가를_숨긴다() throws Exception {
        Candidate saved = save("82", null);
        saved.getPoliceItem().updateDetails(new PoliceItemDetails("변경", null, null, null, null,
                null, null, null, null), NOW.plusMinutes(1));
        items.flush();
        mvc.perform(auth(get(detailUrl(saved)))).andExpect(status().isNotFound());
        mvc.perform(auth(get(listUrl()))).andExpect(jsonPath("$.result.totalElements").value(0));
        Candidate legacy = save("90", null);
        em.createNativeQuery("update candidate set assessed_analysis_id=null, assessed_police_item_version=null where id=:id")
                .setParameter("id", legacy.getId()).executeUpdate();
        em.clear();
        mvc.perform(auth(get(detailUrl(legacy)))).andExpect(status().isNotFound());
        mvc.perform(auth(get(listUrl()))).andExpect(jsonPath("$.result.totalElements").value(0));
    }

    @Test
    void 종료카드의_후보도_조회와_피드백이_가능하다() throws Exception {
        Candidate saved = save("82", null);
        card.close(SearchCardStatus.CLOSED, SearchCardCloseReason.SEARCH_STOPPED, NOW.plusDays(1));
        cards.flush();
        mvc.perform(auth(get(detailUrl(saved)))).andExpect(status().isOk());
        mvc.perform(auth(post(detailUrl(saved) + "/view"))).andExpect(status().isOk());
    }

    @Test
    void 모든_경로에서_비로그인과_타인과_삭제사용자를_거절한다() throws Exception {
        Candidate saved = save("82", null);
        for (String action : List.of("list", "detail", "view", "feedback", "clear")) {
            mvc.perform(request(action, saved)).andExpect(status().isUnauthorized());
            mvc.perform(request(action, saved).with(jwt().jwt(j -> j.subject(stranger.getId().toString())
                            .claim("tokenType", "access"))))
                    .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("COMMON4031"));
            mvc.perform(request(action, saved).with(jwt().jwt(j -> j.subject("999999999")
                            .claim("tokenType", "access"))))
                    .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("AUTH4012"));
        }
        em.clear();
        Candidate untouched = candidates.findById(saved.getId()).orElseThrow();
        assertThat(untouched.getFeedback()).isNull();
        assertThat(untouched.getViewedAt()).isNull();
    }

    @Test
    void 존재하지_않는_리소스는_404다() throws Exception {
        for (var request : List.of(get("/api/v1/search-cards/999999999/candidates"),
                get("/api/v1/candidates/999999999"), post("/api/v1/candidates/999999999/view"),
                delete("/api/v1/candidates/999999999/feedback"),
                put("/api/v1/candidates/999999999/feedback").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"feedback\":\"UNSURE\"}"))) {
            mvc.perform(auth(request)).andExpect(status().isNotFound());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"page=-1", "page=2147483647", "size=0", "size=101", "minScore=-0.1",
            "minScore=100.1", "minScore=NaN", "includeExcluded=maybe", "page=text"})
    void 잘못된_조회조건은_400이다(String param) throws Exception {
        String[] parts = param.split("=");
        mvc.perform(auth(get(listUrl()).param(parts[0], parts[1])))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON4001"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"feedback\":null}"})
    void 필수_피드백_누락은_4004다(String body) throws Exception {
        Candidate saved = save("82", null);
        mvc.perform(auth(put(detailUrl(saved) + "/feedback").contentType(MediaType.APPLICATION_JSON).content(body)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON4004"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"feedback\":\"UNKNOWN\"}", "{\"feedback\":2}", "{broken",
            "{\"feedback\":\"2\"}", "{\"feedback\":true}", "{\"feedback\":[]}"})
    void 잘못된_피드백은_4001이다(String body) throws Exception {
        Candidate saved = save("82", null);
        mvc.perform(auth(put(detailUrl(saved) + "/feedback").contentType(MediaType.APPLICATION_JSON).content(body)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON4001"));
    }

    @Test
    void 다른_카드의_후보가_목록이나_순위에_섞이지_않는다() {
        Candidate saved = save("50", null);
        SearchCard other = cards.saveAndFlush(SearchCard.create(owner.getId(), analysisId(), "WALLET", "다른 카드",
                List.of("BLACK"), null, null, "로고", LocalDate.of(2026, 9, 25), null, null, NOW));
        candidates.saveAndFlush(Candidate.create(other, saved.getPoliceItem(), saved.getScores(),
                new BigDecimal("99"), saved.getEvidenceCoverage(), "m", "p", "s", saved.getEvidence(), NOW));
        assertThat(queries.list(owner.getId(), card.getId(), null, false, 0, 5).totalElements()).isEqualTo(1);
        assertThat(queries.detail(owner.getId(), saved.getId()).rank()).isEqualTo(1);
    }

    private MockHttpServletRequestBuilder request(String action, Candidate candidate) {
        return switch (action) {
            case "list" -> get(listUrl());
            case "detail" -> get(detailUrl(candidate));
            case "view" -> post(detailUrl(candidate) + "/view");
            case "feedback" -> put(detailUrl(candidate) + "/feedback").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"feedback\":\"NOT_MINE\"}");
            default -> delete(detailUrl(candidate) + "/feedback");
        };
    }

    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder request) {
        return request.with(jwt().jwt(j -> j.subject(owner.getId().toString()).claim("tokenType", "access")));
    }

    private String listUrl() { return "/api/v1/search-cards/" + card.getId() + "/candidates"; }
    private String detailUrl(Candidate candidate) { return "/api/v1/candidates/" + candidate.getId(); }

    private Long analysisId() {
        return analyses.saveAndFlush(SearchCardAnalysis.create(owner.getId(),
                new AiAnalysisClientResDTO("WALLET", "CARD_WALLET", List.of("BLACK"), null, List.of(),
                        null, List.of("로고"), "test"))).getId();
    }

    private Candidate save(String total, LocalDate date) {
        PoliceItem item = items.saveAndFlush(PoliceItem.create(PoliceItemSource.POLICE, UUID.randomUUID().toString(),
                1, new PoliceItemDetails("검정 지갑", "OTHER", "BLACK", "공식 설명", date, "판교역",
                "분당경찰서", null, "https://example.com/item"), NOW));
        CandidateScores scores = total == null ? CandidateScores.of(null, null, null, null, null, null)
                : CandidateScores.of(null, new BigDecimal("0.8"), null, BigDecimal.ZERO, null, null);
        return candidates.saveAndFlush(Candidate.create(card, item, scores,
                total == null ? null : new BigDecimal(total), total == null ? BigDecimal.ZERO : new BigDecimal("0.3"),
                "model", "preprocess", "policy",
                List.of(CandidateEvidence.of(EvidenceType.MISSING, ScoreElement.IMAGE, "사진 없음")), NOW));
    }
}
