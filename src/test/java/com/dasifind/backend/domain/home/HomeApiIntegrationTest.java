package com.dasifind.backend.domain.home;

import com.dasifind.backend.domain.candidate.entity.Candidate;
import com.dasifind.backend.domain.candidate.entity.CandidateScores;
import com.dasifind.backend.domain.candidate.model.CandidateFeedback;
import com.dasifind.backend.domain.candidate.repository.CandidateRepository;
import com.dasifind.backend.domain.candidate.service.CandidateSummaryQueryService;
import com.dasifind.backend.domain.home.service.HomeQueryService;
import com.dasifind.backend.domain.policeitem.entity.PoliceItem;
import com.dasifind.backend.domain.policeitem.model.PoliceItemDetails;
import com.dasifind.backend.domain.policeitem.model.PoliceItemSource;
import com.dasifind.backend.domain.policeitem.repository.PoliceItemRepository;
import com.dasifind.backend.domain.searchcard.analysis.client.AiAnalysisClientResDTO;
import com.dasifind.backend.domain.searchcard.analysis.entity.SearchCardAnalysis;
import com.dasifind.backend.domain.searchcard.analysis.repository.SearchCardAnalysisRepository;
import com.dasifind.backend.domain.searchcard.entity.SearchCard;
import com.dasifind.backend.domain.searchcard.entity.LostLocation;
import com.dasifind.backend.domain.searchcard.model.SearchCardStatus;
import com.dasifind.backend.domain.searchcard.model.SearchCardCloseReason;
import com.dasifind.backend.domain.searchcard.repository.SearchCardRepository;
import com.dasifind.backend.domain.searchcard.repository.LostLocationRepository;
import com.dasifind.backend.domain.user.entity.User;
import com.dasifind.backend.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "app.search-card.expiration-initial-delay=1h",
        "app.search-card-image.cleanup-initial-delay=1h",
        "app.search-card-image.deletion-cleanup-initial-delay=1h"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class HomeApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired HomeQueryService home;
    @Autowired CandidateSummaryQueryService summaries;
    @Autowired SearchCardRepository cards;
    @Autowired LostLocationRepository locations;
    @Autowired SearchCardAnalysisRepository analyses;
    @Autowired CandidateRepository candidates;
    @Autowired PoliceItemRepository items;
    @Autowired UserRepository users;
    @Autowired EntityManager em;
    @Autowired EntityManagerFactory emf;
    @MockitoBean Clock homeClock;
    private User owner;
    private User other;
    private static final LocalDateTime NOW = LocalDateTime.of(2030, 9, 29, 12, 0);
    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    @BeforeEach
    void setup() {
        when(homeClock.getZone()).thenReturn(ZONE);
        setNow(NOW);
        owner = users.saveAndFlush(User.create(UUID.randomUUID() + "@example.com", "encoded", "사용자", true));
        other = users.saveAndFlush(User.create(UUID.randomUUID() + "@example.com", "encoded", "다른 사용자", true));
    }

    @Test
    void 비로그인과_없는_사용자는_홈에_접근할_수_없다() throws Exception {
        mvc.perform(get("/api/v1/home")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("COMMON4011"));
        mvc.perform(get("/api/v1/home").with(jwt().jwt(j -> j.subject("999999999"))))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("AUTH4012"));
    }

    @Test
    void 빈_홈은_빈배열과_미확인알림_0을_반환한다() throws Exception {
        mvc.perform(get("/api/v1/home").with(jwt().jwt(j -> j.subject(owner.getId().toString()))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result.activeSearchCards").isEmpty())
                .andExpect(jsonPath("$.result.newCandidates").isEmpty())
                .andExpect(jsonPath("$.result.unreadNotificationCount").value(0));
    }

    @Test
    void 본인의_유효한_ACTIVE카드만_최신순으로_표시한다() throws Exception {
        SearchCard older = card(owner, NOW.minusDays(2));
        SearchCard newer = card(owner, NOW.minusDays(1));
        SearchCard tied = card(owner, NOW.minusDays(1));
        SearchCard expiredPending = card(owner, NOW.minusDays(31));
        SearchCard closed = card(owner, NOW);
        closed.close(SearchCardStatus.CLOSED, SearchCardCloseReason.SEARCH_STOPPED, NOW);
        SearchCard found = card(owner, NOW);
        found.close(SearchCardStatus.FOUND, SearchCardCloseReason.FOUND_OTHER_WAY, NOW);
        SearchCard expired = card(owner, NOW.minusDays(31));
        expired.close(SearchCardStatus.EXPIRED, null, NOW);
        SearchCard foreign = card(other, NOW);
        for (SearchCard hidden : List.of(expiredPending, closed, found, expired, foreign)) save(hidden, "100", null);
        cards.flush();
        em.clear();
        var result = home.getHome(owner.getId());
        assertThat(result.activeSearchCards()).extracting(x -> x.id())
                .containsExactly(tied.getId(), newer.getId(), older.getId());
        assertThat(result.activeSearchCards().getFirst().daysRemaining()).isEqualTo(29);
        assertThat(result.newCandidates()).isEmpty();
        assertThat(cards.findById(expiredPending.getId()).orElseThrow().getStatus()).isEqualTo(SearchCardStatus.ACTIVE);
        mvc.perform(get("/api/v1/home").with(jwt().jwt(j -> j.subject(owner.getId().toString()))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result.activeSearchCards[0].lostPlaceName").value("판교역"))
                .andExpect(jsonPath("$.result.activeSearchCards[0].bestCandidateScore").value(nullValue()));
    }

    @Test
    void 만료당일_남은일수는_0이고_만료시각을_지나면_즉시_숨긴다() {
        SearchCard card = card(owner, NOW.minusDays(30));
        save(card, "82", null);
        setNow(card.getSearchExpiresAt());
        assertThat(home.getHome(owner.getId()).activeSearchCards().getFirst().daysRemaining()).isZero();
        assertThat(home.getHome(owner.getId()).newCandidates()).hasSize(1);
        setNow(card.getSearchExpiresAt().plusNanos(1000));
        assertThat(home.getHome(owner.getId()).activeSearchCards()).isEmpty();
        assertThat(home.getHome(owner.getId()).newCandidates()).isEmpty();
    }

    @Test
    void 새후보는_전체_활성카드에서_적합도순_최대5개이며_조회로_읽음되지_않는다() {
        SearchCard first = card(owner, NOW);
        SearchCard second = card(owner, NOW);
        Candidate noDate = save(first, "90", null);
        Candidate older = save(second, "90", LocalDate.of(2030, 9, 26));
        Candidate tie1 = save(first, "90", LocalDate.of(2030, 9, 27));
        Candidate tie2 = save(second, "90", LocalDate.of(2030, 9, 27));
        Candidate best = save(second, "95", null);
        save(first, "10", null);
        em.clear();
        var result = home.getHome(owner.getId());
        assertThat(result.newCandidates()).extracting(x -> x.id())
                .containsExactly(best.getId(), tie1.getId(), tie2.getId(), older.getId(), noDate.getId());
        assertThat(result.newCandidates()).allMatch(x -> x.isNew());
        assertThat(candidates.findById(best.getId()).orElseThrow().getViewedAt()).isNull();
    }

    @Test
    void 읽음은_홈새후보만_제거하고_제외는_모든_집계에_반영된다() {
        SearchCard card = card(owner, NOW);
        Candidate best = save(card, "90", null);
        Candidate second = save(card, "80", null);
        best.markViewed(NOW);
        candidates.flush();
        assertThat(home.getHome(owner.getId()).newCandidates()).extracting(x -> x.id()).containsExactly(second.getId());
        assertThat(stats(card).candidateCount()).isEqualTo(2);
        assertThat(stats(card).bestCandidateScore()).isEqualByComparingTo("90");
        best.updateFeedback(CandidateFeedback.NOT_MINE);
        candidates.flush();
        assertThat(stats(card).candidateCount()).isEqualTo(1);
        assertThat(home.getHome(owner.getId()).activeSearchCards().getFirst().bestCandidateScore()).isEqualByComparingTo("80");
        best.clearFeedback();
        candidates.flush();
        assertThat(stats(card).candidateCount()).isEqualTo(2);
        assertThat(stats(card).bestCandidateScore()).isEqualByComparingTo("90");
        assertThat(home.getHome(owner.getId()).newCandidates()).extracting(x -> x.id()).containsExactly(second.getId());
    }

    @Test
    void 미계산_제외_오래된_후보를_빼고_실제0점은_포함한다() throws Exception {
        SearchCard card = card(owner, NOW);
        Candidate zero = save(card, "0", null);
        save(card, null, null);
        Candidate excluded = save(card, "100", null);
        excluded.updateFeedback(CandidateFeedback.NOT_MINE);
        Candidate stale = save(card, "99", null);
        stale.getPoliceItem().updateDetails(new PoliceItemDetails("변경", null, null, null, null, null, null, null, null), NOW.plusMinutes(1));
        items.flush();
        Candidate legacy = save(card, "98", null);
        em.createNativeQuery("update candidate set assessed_analysis_id=null, assessed_police_item_version=null where id=:id")
                .setParameter("id", legacy.getId()).executeUpdate();
        em.clear();
        assertThat(stats(card).candidateCount()).isEqualTo(1);
        assertThat(stats(card).bestCandidateScore()).isEqualByComparingTo("0");
        assertThat(home.getHome(owner.getId()).newCandidates()).extracting(x -> x.id()).containsExactly(zero.getId());
        assertSummaryEndpoints(card, 1, 0);
    }

    @Test
    void 후보개수는_5개제한전_전체이고_상세와_목록에_같은최고점수를_반영한다() throws Exception {
        SearchCard card = card(owner, NOW);
        for (int i = 0; i < 7; i++) save(card, Integer.toString(90 - i), null);
        em.clear();
        assertSummaryEndpoints(card, 7, 90);
        mvc.perform(get("/api/v1/search-cards/" + card.getId() + "/candidates")
                        .with(jwt().jwt(j -> j.subject(owner.getId().toString()))))
                .andExpect(jsonPath("$.result.totalElements").value(7))
                .andExpect(jsonPath("$.result.content.length()").value(5));
    }

    @Test
    void 카드수정후_집계는_0과_null이고_기존_점수를_홈에_노출하지_않는다() throws Exception {
        SearchCard card = card(owner, NOW);
        save(card, "90", null);
        card.update(analysis(owner), "WALLET", "수정", List.of("NAVY"), null, null, "특징",
                NOW.toLocalDate(), null, null, NOW.plusMinutes(1));
        cards.flush();
        em.clear();
        assertThat(stats(card).candidateCount()).isZero();
        assertThat(stats(card).bestCandidateScore()).isNull();
        var result = home.getHome(owner.getId());
        assertThat(result.activeSearchCards().getFirst().bestCandidateScore()).isNull();
        assertThat(result.newCandidates()).isEmpty();
        mvc.perform(get("/api/v1/search-cards/" + card.getId()).with(jwt().jwt(j -> j.subject(owner.getId().toString()))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result.candidateCount").value(0))
                .andExpect(jsonPath("$.result.bestCandidateScore").value(nullValue()));
    }

    @Test
    void 타인카드_ID가_집계요청에_섞여도_유출하지_않는다() {
        SearchCard mine = card(owner, NOW);
        SearchCard foreign = card(other, NOW);
        save(mine, "10", null);
        save(foreign, "100", null);
        var result = summaries.summarize(owner.getId(), List.of(mine.getId(), foreign.getId()));
        assertThat(result).containsOnlyKeys(mine.getId());
        assertThat(result.get(mine.getId()).bestCandidateScore()).isEqualByComparingTo("10");
    }

    @Test
    void 종료카드의_상세집계는_유지되지만_홈에서는_숨긴다() throws Exception {
        SearchCard card = card(owner, NOW);
        save(card, "90", null);
        card.close(SearchCardStatus.CLOSED, SearchCardCloseReason.SEARCH_STOPPED, NOW);
        cards.flush();
        assertThat(home.getHome(owner.getId()).activeSearchCards()).isEmpty();
        assertSummaryEndpoints(card, 1, 90);
    }

    @Test
    void 카드가_많아져도_홈_조회쿼리는_일괄처리된다() {
        for (int i = 0; i < 12; i++) save(card(owner, NOW), "80", null);
        em.clear();
        var statistics = emf.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        assertThat(home.getHome(owner.getId()).activeSearchCards()).hasSize(12);
        assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(6);
    }

    private void assertSummaryEndpoints(SearchCard card, int count, int maxScore) throws Exception {
        mvc.perform(get("/api/v1/search-cards/" + card.getId()).with(jwt().jwt(j -> j.subject(owner.getId().toString()))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result.candidateCount").value(count))
                .andExpect(jsonPath("$.result.bestCandidateScore").value(maxScore));
        mvc.perform(get("/api/v1/search-cards").with(jwt().jwt(j -> j.subject(owner.getId().toString()))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result.content[0].bestCandidateScore").value(maxScore));
    }

    private CandidateSummaryQueryService.Summary stats(SearchCard card) {
        return summaries.summarize(owner.getId(), List.of(card.getId()))
                .getOrDefault(card.getId(), CandidateSummaryQueryService.Summary.EMPTY);
    }

    private void setNow(LocalDateTime now) {
        when(homeClock.instant()).thenReturn(now.atZone(ZONE).toInstant());
    }

    private Long analysis(User user) {
        return analyses.saveAndFlush(SearchCardAnalysis.create(user.getId(),
                new AiAnalysisClientResDTO("WALLET", "CARD_WALLET", List.of("BLACK"), null,
                        List.of(), null, List.of("로고"), "test"))).getId();
    }

    private SearchCard card(User user, LocalDateTime createdAt) {
        SearchCard card = cards.saveAndFlush(SearchCard.create(user.getId(), analysis(user), "WALLET", "지갑",
                List.of("BLACK"), null, null, "로고", createdAt.toLocalDate(), null, null, createdAt));
        locations.saveAndFlush(LostLocation.create(card.getId(), "판교역", "성남", new BigDecimal("37.39"),
                new BigDecimal("127.11"), null, createdAt));
        return card;
    }

    private Candidate save(SearchCard card, String total, LocalDate foundDate) {
        PoliceItem item = items.saveAndFlush(PoliceItem.create(PoliceItemSource.POLICE, UUID.randomUUID().toString(), 1,
                new PoliceItemDetails("검정 지갑", "WALLET", "BLACK", "설명", foundDate, "판교역", "분당경찰서", null, null), NOW));
        return candidates.saveAndFlush(Candidate.create(card, item,
                CandidateScores.of(null, total == null ? null : new BigDecimal("0.8"), null, null, null, null),
                total == null ? null : new BigDecimal(total), total == null ? BigDecimal.ZERO : new BigDecimal("0.2"),
                "m", "p", "s", List.of(), NOW));
    }
}
