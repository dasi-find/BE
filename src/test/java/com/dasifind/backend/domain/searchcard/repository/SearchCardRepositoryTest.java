package com.dasifind.backend.domain.searchcard.repository;

import com.dasifind.backend.domain.searchcard.analysis.client.AiAnalysisClientResDTO;
import com.dasifind.backend.domain.searchcard.analysis.entity.SearchCardAnalysis;
import com.dasifind.backend.domain.searchcard.analysis.repository.SearchCardAnalysisRepository;
import com.dasifind.backend.domain.searchcard.entity.SearchCard;
import com.dasifind.backend.domain.searchcard.model.SearchCardStatus;
import com.dasifind.backend.domain.searchcard.model.SearchCardCloseReason;
import com.dasifind.backend.domain.user.entity.User;
import com.dasifind.backend.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SearchCardRepositoryTest {

    @Autowired
    private SearchCardRepository searchCardRepository;

    @Autowired
    private SearchCardAnalysisRepository analysisRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void 만료_시각을_지난_ACTIVE만_갱신하고_재실행해도_종료정보를_유지한다() {
        User owner = userRepository.save(User.create(
                "expiration@example.com", "encoded", "민준", true));
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 1, 12, 0);
        SearchCard due = saveSearchCard(owner, "만료 대상", SearchCardStatus.ACTIVE, createdAt);
        SearchCard future = saveSearchCard(owner, "유효", SearchCardStatus.ACTIVE, createdAt.plusDays(1));
        SearchCard found = saveSearchCard(owner, "찾음", SearchCardStatus.ACTIVE, createdAt);
        SearchCard closed = saveSearchCard(owner, "중단", SearchCardStatus.ACTIVE, createdAt);
        LocalDateTime manuallyClosedAt = createdAt.plusDays(2);
        found.close(SearchCardStatus.FOUND, SearchCardCloseReason.FOUND_OTHER_WAY, manuallyClosedAt);
        closed.close(SearchCardStatus.CLOSED, SearchCardCloseReason.SEARCH_STOPPED, manuallyClosedAt);
        LocalDateTime boundary = due.getSearchExpiresAt();

        assertThat(searchCardRepository.expireDueCards(boundary)).isZero();
        LocalDateTime now = boundary.plusSeconds(1);
        assertThat(searchCardRepository.expireDueCards(now)).isEqualTo(1);

        SearchCard expired = searchCardRepository.findById(due.getId()).orElseThrow();
        assertThat(expired.getStatus()).isEqualTo(SearchCardStatus.EXPIRED);
        assertThat(expired.getClosedAt()).isEqualTo(boundary);
        assertThat(expired.getUpdatedAt()).isEqualTo(now);
        assertThat(expired.getCloseReason()).isNull();
        assertThat(expired.getColors()).containsExactly("BLACK");
        assertThat(analysisRepository.existsById(expired.getAnalysisId())).isTrue();
        assertThat(searchCardRepository.findById(future.getId()).orElseThrow().getStatus())
                .isEqualTo(SearchCardStatus.ACTIVE);
        SearchCard preservedFound = searchCardRepository.findById(found.getId()).orElseThrow();
        assertThat(preservedFound.getStatus()).isEqualTo(SearchCardStatus.FOUND);
        assertThat(preservedFound.getCloseReason()).isEqualTo(SearchCardCloseReason.FOUND_OTHER_WAY);
        assertThat(preservedFound.getClosedAt()).isEqualTo(manuallyClosedAt);
        SearchCard preservedClosed = searchCardRepository.findById(closed.getId()).orElseThrow();
        assertThat(preservedClosed.getStatus()).isEqualTo(SearchCardStatus.CLOSED);
        assertThat(preservedClosed.getCloseReason()).isEqualTo(SearchCardCloseReason.SEARCH_STOPPED);
        assertThat(preservedClosed.getClosedAt()).isEqualTo(manuallyClosedAt);

        assertThat(searchCardRepository.expireDueCards(now.plusSeconds(1))).isZero();
        assertThat(searchCardRepository.findById(due.getId()).orElseThrow().getUpdatedAt()).isEqualTo(now);
    }

    @Test
    void 본인의_수색카드만_최근_생성순으로_조회한다() {
        User owner = userRepository.save(User.create(
                "list-owner@example.com",
                "encoded-password",
                "민준",
                true
        ));
        User other = userRepository.save(User.create(
                "list-other@example.com",
                "encoded-password",
                "다른 사용자",
                true
        ));

        SearchCard older = saveSearchCard(
                owner,
                "오래된 카드",
                SearchCardStatus.ACTIVE,
                LocalDateTime.of(2026, 8, 1, 10, 0)
        );
        SearchCard newest = saveSearchCard(
                owner,
                "최근 카드",
                SearchCardStatus.CLOSED,
                LocalDateTime.of(2026, 8, 2, 10, 0)
        );
        saveSearchCard(
                other,
                "타인 카드",
                SearchCardStatus.ACTIVE,
                LocalDateTime.of(2026, 8, 3, 10, 0)
        );

        PageRequest pageable = PageRequest.of(
                0,
                20,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))
        );
        Page<SearchCard> result = searchCardRepository.findByUserId(owner.getId(), pageable);

        assertThat(result.getContent()).extracting(SearchCard::getId)
                .containsExactly(newest.getId(), older.getId());
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    void 상태를_지정하면_본인의_해당_상태_수색카드만_조회한다() {
        User owner = userRepository.save(User.create(
                "status-owner@example.com",
                "encoded-password",
                "민준",
                true
        ));
        saveSearchCard(
                owner,
                "진행 중 카드",
                SearchCardStatus.ACTIVE,
                LocalDateTime.of(2026, 8, 1, 10, 0)
        );
        SearchCard closed = saveSearchCard(
                owner,
                "종료 카드",
                SearchCardStatus.CLOSED,
                LocalDateTime.of(2026, 8, 2, 10, 0)
        );

        Page<SearchCard> result = searchCardRepository.findByUserIdAndStatus(
                owner.getId(),
                SearchCardStatus.CLOSED,
                PageRequest.of(0, 20)
        );

        assertThat(result.getContent()).extracting(SearchCard::getId)
                .containsExactly(closed.getId());
    }

    private SearchCard saveSearchCard(
            User user,
            String itemName,
            SearchCardStatus status,
            LocalDateTime createdAt
    ) {
        SearchCardAnalysis analysis = analysisRepository.save(SearchCardAnalysis.create(
                user.getId(),
                new AiAnalysisClientResDTO(
                        "WALLET",
                        "CARD_WALLET",
                        List.of("BLACK"),
                        null,
                        List.of("LEATHER"),
                        null,
                        List.of(),
                        "preprocess-v1"
                )
        ));
        SearchCard searchCard = SearchCard.create(
                user.getId(),
                analysis.getId(),
                "WALLET",
                itemName,
                List.of("BLACK"),
                null,
                "LEATHER",
                "특징 없음",
                LocalDate.of(2026, 7, 31),
                null,
                null,
                createdAt
        );
        ReflectionTestUtils.setField(searchCard, "status", status);
        return searchCardRepository.saveAndFlush(searchCard);
    }
}
