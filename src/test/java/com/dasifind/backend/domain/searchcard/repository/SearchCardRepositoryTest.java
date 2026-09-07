package com.dasifind.backend.domain.searchcard.repository;

import com.dasifind.backend.domain.searchcard.analysis.client.AiAnalysisClientResponse;
import com.dasifind.backend.domain.searchcard.analysis.entity.SearchCardAnalysis;
import com.dasifind.backend.domain.searchcard.analysis.repository.SearchCardAnalysisRepository;
import com.dasifind.backend.domain.searchcard.entity.SearchCard;
import com.dasifind.backend.domain.searchcard.model.SearchCardStatus;
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
                new AiAnalysisClientResponse(
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
