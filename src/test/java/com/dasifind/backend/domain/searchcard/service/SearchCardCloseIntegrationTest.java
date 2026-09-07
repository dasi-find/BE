package com.dasifind.backend.domain.searchcard.service;

import com.dasifind.backend.domain.searchcard.analysis.client.AiAnalysisClientResponse;
import com.dasifind.backend.domain.searchcard.analysis.entity.SearchCardAnalysis;
import com.dasifind.backend.domain.searchcard.analysis.repository.SearchCardAnalysisRepository;
import com.dasifind.backend.domain.searchcard.dto.request.SearchCardCloseRequest;
import com.dasifind.backend.domain.searchcard.dto.response.SearchCardCloseResponse;
import com.dasifind.backend.domain.searchcard.entity.SearchCard;
import com.dasifind.backend.domain.searchcard.model.SearchCardCloseReason;
import com.dasifind.backend.domain.searchcard.model.SearchCardStatus;
import com.dasifind.backend.domain.searchcard.repository.SearchCardRepository;
import com.dasifind.backend.domain.user.entity.User;
import com.dasifind.backend.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SearchCardCloseIntegrationTest {

    @Autowired
    private SearchCardCloseService service;

    @Autowired
    private SearchCardRepository searchCardRepository;

    @Autowired
    private SearchCardAnalysisRepository searchCardAnalysisRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void 종료_상태와_사유와_시각을_DB에_저장한다() {
        User user = userRepository.saveAndFlush(User.create(
                "close-owner@example.com",
                "encoded-password",
                "민준",
                true
        ));
        SearchCardAnalysis analysis = searchCardAnalysisRepository.saveAndFlush(
                SearchCardAnalysis.create(
                        user.getId(),
                        new AiAnalysisClientResponse(
                                "WALLET",
                                "CARD_WALLET",
                                List.of("NAVY"),
                                null,
                                List.of("LEATHER"),
                                null,
                                List.of("앞면 은색 로고"),
                                "preprocess-v1"
                        )
                )
        );
        SearchCard searchCard = searchCardRepository.saveAndFlush(SearchCard.create(
                user.getId(),
                analysis.getId(),
                "WALLET",
                "남색 카드지갑",
                List.of("NAVY"),
                null,
                "LEATHER",
                "앞면 은색 로고",
                LocalDate.of(2026, 8, 17),
                null,
                null,
                LocalDateTime.of(2026, 8, 18, 10, 0)
        ));

        SearchCardCloseResponse response = service.close(
                user.getId(),
                searchCard.getId(),
                new SearchCardCloseRequest(
                        SearchCardStatus.FOUND,
                        SearchCardCloseReason.FOUND_BY_RECOMMENDATION
                )
        );
        entityManager.flush();
        entityManager.clear();

        SearchCard closedCard = searchCardRepository.findById(searchCard.getId()).orElseThrow();
        assertThat(closedCard.getStatus()).isEqualTo(SearchCardStatus.FOUND);
        assertThat(closedCard.getCloseReason())
                .isEqualTo(SearchCardCloseReason.FOUND_BY_RECOMMENDATION);
        assertThat(closedCard.getClosedAt()).isEqualTo(response.closedAt());
        assertThat(closedCard.getUpdatedAt()).isEqualTo(response.closedAt());
    }
}
