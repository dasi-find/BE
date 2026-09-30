package com.dasifind.backend.domain.notification;

import com.dasifind.backend.domain.notification.entity.Notification;
import com.dasifind.backend.domain.notification.model.NotificationType;
import com.dasifind.backend.domain.notification.repository.NotificationRepository;
import com.dasifind.backend.domain.notification.service.NotificationService;
import com.dasifind.backend.domain.candidate.entity.Candidate;
import com.dasifind.backend.domain.candidate.entity.CandidateScores;
import com.dasifind.backend.domain.candidate.repository.CandidateRepository;
import com.dasifind.backend.domain.policeitem.entity.PoliceItem;
import com.dasifind.backend.domain.policeitem.model.PoliceItemDetails;
import com.dasifind.backend.domain.policeitem.model.PoliceItemSource;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class NotificationApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired NotificationRepository notifications;
    @Autowired NotificationService service;
    @Autowired UserRepository users;
    @Autowired CandidateRepository candidates;
    @Autowired PoliceItemRepository items;
    @Autowired SearchCardRepository cards;
    @Autowired SearchCardAnalysisRepository analyses;
    @Autowired SearchCardDeleteService deleteService;
    @Autowired EntityManager em;
    User owner;
    User other;
    static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 29, 12, 0);

    @BeforeEach
    void setup() {
        owner = users.saveAndFlush(User.create(UUID.randomUUID() + "@example.com", "encoded", "사용자", true));
        other = users.saveAndFlush(User.create(UUID.randomUUID() + "@example.com", "encoded", "다른 사용자", true));
    }

    @Test
    void 빈목록_기본페이지와_0개집계를_반환한다() throws Exception {
        mvc.perform(get("/api/v1/notifications").with(jwt().jwt(j -> j.subject(owner.getId().toString()))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result.content").isEmpty())
                .andExpect(jsonPath("$.result.page").value(0)).andExpect(jsonPath("$.result.size").value(20))
                .andExpect(jsonPath("$.result.totalElements").value(0)).andExpect(jsonPath("$.result.hasNext").value(false));
        assertCounts(0);
    }

    @Test
    void 본인알림만_생성시각_ID역순으로_페이지조회한다() throws Exception {
        system(other, NOW.plusDays(1));
        Notification first = system(owner, NOW);
        Notification second = system(owner, NOW);
        Notification newest = system(owner, NOW.plusMinutes(1));
        em.clear();
        var page = service.list(owner.getId(), false, 0, 2);
        assertThat(page.content()).extracting(x -> x.id()).containsExactly(newest.getId(), second.getId());
        assertThat(page.totalElements()).isEqualTo(3);
        assertThat(page.hasNext()).isTrue();
        var next = service.list(owner.getId(), false, 1, 2);
        assertThat(next.content()).extracting(x -> x.id()).containsExactly(first.getId());
        assertThat(next.hasNext()).isFalse();
        assertThat(service.list(owner.getId(), false, 5, 2).content()).isEmpty();
        mvc.perform(get("/api/v1/notifications").with(jwt().jwt(j -> j.subject(owner.getId().toString()))))
                .andExpect(jsonPath("$.result.content[0].type").value("SYSTEM"))
                .andExpect(jsonPath("$.result.content[0].referenceType").value(nullValue()))
                .andExpect(jsonPath("$.result.content[0].referenceId").value(nullValue()))
                .andExpect(jsonPath("$.result.content[0].isRead").value(false))
                .andExpect(jsonPath("$.result.content[0].user").doesNotExist());
        assertCounts(3);
    }

    @Test
    void 반복_읽음은_최초시간을_보존하고_미확인필터에_반영한다() throws Exception {
        Notification read = system(owner, NOW);
        Notification unread = system(owner, NOW);
        mvc.perform(post("/api/v1/notifications/" + read.getId() + "/read")
                        .with(jwt().jwt(j -> j.subject(owner.getId().toString()))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result.notificationId").value(read.getId()))
                .andExpect(jsonPath("$.result.isRead").value(true));
        var firstTime = notifications.findById(read.getId()).orElseThrow().getReadAt();
        service.read(owner.getId(), read.getId());
        assertThat(notifications.findById(read.getId()).orElseThrow().getReadAt()).isEqualTo(firstTime);
        mvc.perform(get("/api/v1/notifications").param("unreadOnly", "true")
                        .with(jwt().jwt(j -> j.subject(owner.getId().toString()))))
                .andExpect(jsonPath("$.result.totalElements").value(1))
                .andExpect(jsonPath("$.result.content[0].id").value(unread.getId()));
        assertCounts(1);
    }

    @Test
    void 전체읽음은_미확인만_갱신하며_타인과_신규알림을_건드리지_않는다() throws Exception {
        Notification alreadyRead = system(owner, NOW);
        system(owner, NOW);
        system(owner, NOW);
        Notification foreign = system(other, NOW);
        service.read(owner.getId(), alreadyRead.getId());
        var firstTime = notifications.findById(alreadyRead.getId()).orElseThrow().getReadAt();
        mvc.perform(post("/api/v1/notifications/read-all")
                        .with(jwt().jwt(j -> j.subject(owner.getId().toString()))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result.updatedCount").value(2));
        assertThat(service.readAll(owner.getId()).updatedCount()).isZero();
        assertThat(notifications.findById(alreadyRead.getId()).orElseThrow().getReadAt()).isEqualTo(firstTime);
        assertThat(notifications.findById(foreign.getId()).orElseThrow().isRead()).isFalse();
        assertCounts(0);
        system(owner, NOW.plusHours(1));
        assertCounts(1);
    }

    @Test
    void 모든경로는_인증과_존재하는_사용자를_요구한다() throws Exception {
        var note = system(owner, NOW);
        for (boolean absentUser : List.of(false, true)) {
            var requests = List.of(get("/api/v1/notifications"), get("/api/v1/notifications/unread-count"),
                    post("/api/v1/notifications/read-all"), post("/api/v1/notifications/" + note.getId() + "/read"));
            for (var request : requests) {
                if (absentUser) request.with(jwt().jwt(j -> j.subject("999999999")));
                mvc.perform(request).andExpect(status().isUnauthorized())
                        .andExpect(jsonPath("$.code").value(absentUser ? "AUTH4012" : "COMMON4011"));
            }
        }
        assertThat(notifications.findById(note.getId()).orElseThrow().isRead()).isFalse();
    }

    @Test
    void 다른사용자의_알림은_403_없는알림은_404다() throws Exception {
        Notification foreign = system(other, NOW);
        mvc.perform(post("/api/v1/notifications/" + foreign.getId() + "/read")
                        .with(jwt().jwt(j -> j.subject(owner.getId().toString()))))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("COMMON4031"));
        mvc.perform(post("/api/v1/notifications/999999999/read")
                        .with(jwt().jwt(j -> j.subject(owner.getId().toString()))))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("COMMON4041"));
        assertThat(notifications.findById(foreign.getId()).orElseThrow().isRead()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"page=-1", "page=2147483647", "size=0", "size=101", "unreadOnly=maybe", "page=abc"})
    void 잘못된_쿼리를_400으로_거절한다(String input) throws Exception {
        String[] parts = input.split("=");
        mvc.perform(get("/api/v1/notifications").param(parts[0], parts[1])
                        .with(jwt().jwt(j -> j.subject(owner.getId().toString()))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON4001"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "abc"})
    void 잘못된_알림ID를_거절한다(String id) throws Exception {
        mvc.perform(post("/api/v1/notifications/" + id + "/read")
                        .with(jwt().jwt(j -> j.subject(owner.getId().toString()))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 후보와_카드_딥링크를_반환하고_읽음은_후보확인과_독립적이다() throws Exception {
        Candidate candidate = candidate();
        Notification note = notifications.saveAndFlush(Notification.forCandidate(owner, candidate, "유사한 후보", "확인해 주세요", NOW));
        notifications.saveAndFlush(Notification.forSearchCard(owner, candidate.getSearchCard(),
                NotificationType.SEARCH_EXPIRING, "만료 예정", "3일 뒤 종료됩니다", NOW.minusMinutes(1)));
        em.clear();
        mvc.perform(get("/api/v1/notifications").with(jwt().jwt(j -> j.subject(owner.getId().toString()))))
                .andExpect(jsonPath("$.result.content[0].referenceType").value("CANDIDATE"))
                .andExpect(jsonPath("$.result.content[0].referenceId").value(candidate.getId()))
                .andExpect(jsonPath("$.result.content[1].referenceType").value("SEARCH_CARD"));
        service.read(owner.getId(), note.getId());
        Candidate loaded = candidates.findById(candidate.getId()).orElseThrow();
        assertThat(loaded.getViewedAt()).isNull();
        assertThat(loaded.getFeedback()).isNull();
        assertThat(loaded.isNotificationSuppressed()).isFalse();
        assertCounts(1);
    }

    @Test
    void 후보확인은_알림읽음을_바꾸지_않는다() throws Exception {
        Candidate candidate = candidate();
        notifications.saveAndFlush(Notification.forCandidate(owner, candidate, "유사한 후보", "확인해 주세요", NOW));
        candidate.markViewed(NOW);
        candidates.flush();
        assertCounts(1);
    }

    @Test
    void 카드삭제는_관련알림만_삭제하고_시스템알림과_습득물은_보존한다() throws Exception {
        Candidate candidate = candidate();
        Long itemId = candidate.getPoliceItem().getId();
        Long cardId = candidate.getSearchCard().getId();
        notifications.saveAndFlush(Notification.forCandidate(owner, candidate, "유사한 후보", "확인", NOW));
        notifications.saveAndFlush(Notification.forSearchCard(owner, candidate.getSearchCard(),
                NotificationType.SEARCH_EXPIRED, "기간 종료", "종료됨", NOW));
        Notification system = system(owner, NOW);
        Notification foreign = system(other, NOW);
        deleteService.delete(owner.getId(), cardId);
        em.clear();
        assertThat(notifications.findAll()).extracting(Notification::getId).containsExactlyInAnyOrder(system.getId(), foreign.getId());
        assertThat(items.existsById(itemId)).isTrue();
        assertCounts(1);
    }

    @Test
    void 사용자삭제는_해당사용자_시스템알림만_삭제한다() {
        system(owner, NOW);
        Notification foreign = system(other, NOW);
        users.deleteById(owner.getId());
        users.flush();
        em.clear();
        assertThat(notifications.findAll()).extracting(Notification::getId).containsExactly(foreign.getId());
    }

    @Test
    void 타인참조와_잘못된_알림유형_빈본문을_생성단계에서_거절한다() {
        Candidate candidate = candidate();
        assertThatThrownBy(() -> Notification.forCandidate(other, candidate, "제목", "내용", NOW))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Notification.forSearchCard(other, candidate.getSearchCard(),
                NotificationType.SEARCH_EXPIRED, "제목", "내용", NOW)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Notification.forSearchCard(owner, candidate.getSearchCard(),
                NotificationType.SYSTEM, "제목", "내용", NOW)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Notification.system(owner, " ", "내용", NOW)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Notification.system(owner, "제목", "x".repeat(2001), NOW)).isInstanceOf(IllegalArgumentException.class);
    }

    private void assertCounts(int expected) throws Exception {
        for (String url : List.of("/api/v1/notifications/unread-count", "/api/v1/home")) {
            mvc.perform(get(url).with(jwt().jwt(j -> j.subject(owner.getId().toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath(url.endsWith("home") ? "$.result.unreadNotificationCount" : "$.result.unreadCount").value(expected));
        }
    }

    Notification system(User user, LocalDateTime now) {
        return notifications.saveAndFlush(Notification.system(user, "시스템 안내", "안내 내용", now));
    }

    Candidate candidate() {
        var analysis = analyses.saveAndFlush(SearchCardAnalysis.create(owner.getId(),
                new AiAnalysisClientResDTO("WALLET", "CARD_WALLET", List.of("BLACK"), null, List.of(),
                        null, List.of("로고"), "test")));
        var card = cards.saveAndFlush(SearchCard.create(owner.getId(), analysis.getId(), "WALLET", "지갑",
                List.of("BLACK"), null, null, "로고", NOW.toLocalDate(), null, null, NOW));
        var item = items.saveAndFlush(PoliceItem.create(PoliceItemSource.POLICE, UUID.randomUUID().toString(), 1,
                new PoliceItemDetails("지갑", null, null, null, null, null, null, null, null), NOW));
        return candidates.saveAndFlush(Candidate.create(card, item,
                CandidateScores.of(null, BigDecimal.ONE, null, null, null, null), BigDecimal.TEN, BigDecimal.ONE,
                "m", "p", "s", List.of(), NOW));
    }
}
