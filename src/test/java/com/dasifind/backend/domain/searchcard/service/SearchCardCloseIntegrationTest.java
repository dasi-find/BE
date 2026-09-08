package com.dasifind.backend.domain.searchcard.service;

import com.dasifind.backend.domain.searchcard.analysis.client.AiAnalysisClientResDTO;
import com.dasifind.backend.domain.searchcard.analysis.entity.SearchCardAnalysis;
import com.dasifind.backend.domain.searchcard.analysis.repository.SearchCardAnalysisRepository;
import com.dasifind.backend.domain.searchcard.dto.request.SearchCardCloseReqDTO;
import com.dasifind.backend.domain.searchcard.dto.response.SearchCardCloseResDTO;
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
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

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
                        new AiAnalysisClientResDTO(
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
                LocalDateTime.now()
        ));

        SearchCardCloseResDTO response = service.close(
                user.getId(),
                searchCard.getId(),
                new SearchCardCloseReqDTO(
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
        assertThat(closedCard.getClosedAt())
                .isCloseTo(response.closedAt(), within(1, ChronoUnit.MICROS));
        assertThat(closedCard.getUpdatedAt()).isEqualTo(closedCard.getClosedAt());
    }
}
