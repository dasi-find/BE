package com.dasifind.backend.domain.searchcard.service;

import com.dasifind.backend.domain.searchcard.analysis.client.AiAnalysisClientResDTO;
import com.dasifind.backend.domain.searchcard.analysis.entity.SearchCardAnalysis;
import com.dasifind.backend.domain.searchcard.analysis.repository.SearchCardAnalysisRepository;
import com.dasifind.backend.domain.searchcard.dto.request.SearchCardLostLocationReqDTO;
import com.dasifind.backend.domain.searchcard.dto.request.SearchCardUpdateReqDTO;
import com.dasifind.backend.domain.searchcard.dto.response.SearchCardUpdateResDTO;
import com.dasifind.backend.domain.searchcard.entity.LostLocation;
import com.dasifind.backend.domain.searchcard.entity.SearchCard;
import com.dasifind.backend.domain.searchcard.image.entity.SearchCardImage;
import com.dasifind.backend.domain.searchcard.image.model.SearchCardImageType;
import com.dasifind.backend.domain.searchcard.image.repository.SearchCardImageRepository;
import com.dasifind.backend.domain.searchcard.repository.LostLocationRepository;
import com.dasifind.backend.domain.searchcard.repository.SearchCardRepository;
import com.dasifind.backend.domain.user.entity.User;
import com.dasifind.backend.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SearchCardUpdateIntegrationTest {

    @Autowired
    private SearchCardUpdateService service;

    @Autowired
    private SearchCardRepository searchCardRepository;

    @Autowired
    private LostLocationRepository lostLocationRepository;

    @Autowired
    private SearchCardAnalysisRepository searchCardAnalysisRepository;

    @Autowired
    private SearchCardImageRepository searchCardImageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void 새_분석_결과로_카드와_분실위치를_교체하고_기존_이미지를_유지한다() {
        User user = userRepository.saveAndFlush(User.create(
                "update-owner@example.com",
                "encoded-password",
                "민준",
                true
        ));
        SearchCardAnalysis oldAnalysis = saveAnalysis(user.getId(), "BLACK");
        SearchCardAnalysis newAnalysis = saveAnalysis(user.getId(), "NAVY");
        LocalDateTime now = LocalDateTime.of(2026, 8, 18, 10, 0);
        SearchCard searchCard = searchCardRepository.saveAndFlush(SearchCard.create(
                user.getId(),
                oldAnalysis.getId(),
                "WALLET",
                "기존 카드지갑",
                List.of("BLACK"),
                null,
                null,
                "기존 특징",
                LocalDate.of(2026, 8, 16),
                null,
                null,
                now
        ));
        lostLocationRepository.saveAndFlush(LostLocation.create(
                searchCard.getId(),
                "기존 위치",
                "기존 주소",
                new BigDecimal("37.3900000"),
                new BigDecimal("127.1100000"),
                null,
                now
        ));
        SearchCardImage image = SearchCardImage.create(
                user.getId(),
                "search-card-images/update/reference.jpg",
                SearchCardImageType.REFERENCE,
                "image/jpeg",
                1024
        );
        image.attachTo(searchCard.getId());
        searchCardImageRepository.saveAndFlush(image);

        SearchCardUpdateResDTO response = service.update(
                user.getId(),
                searchCard.getId(),
                request(newAnalysis.getId())
        );
        entityManager.flush();
        entityManager.clear();

        SearchCard updatedCard = searchCardRepository.findById(searchCard.getId()).orElseThrow();
        LostLocation updatedLocation = lostLocationRepository
                .findBySearchCardId(searchCard.getId())
                .orElseThrow();
        SearchCardImage maintainedImage = searchCardImageRepository.findById(image.getId())
                .orElseThrow();

        assertThat(response.rematchScheduled()).isFalse();
        assertThat(updatedCard.getAnalysisId()).isEqualTo(newAnalysis.getId());
        assertThat(updatedCard.getItemName()).isEqualTo("남색 카드지갑");
        assertThat(updatedCard.getColors()).containsExactly("NAVY", "BLACK");
        assertThat(updatedCard.getLostEndTime()).isEqualTo(LocalTime.of(21, 0));
        assertThat(updatedLocation.getPlaceName()).isEqualTo("판교역 스타벅스");
        assertThat(updatedLocation.getDescription()).isEqualTo("카페에서 마지막으로 사용했습니다.");
        assertThat(maintainedImage.getSearchCardId()).isEqualTo(searchCard.getId());
        assertThat(searchCardAnalysisRepository.findById(oldAnalysis.getId())).isEmpty();
        assertThat(searchCardAnalysisRepository.findById(newAnalysis.getId())).isPresent();
    }

    private SearchCardAnalysis saveAnalysis(Long userId, String color) {
        return searchCardAnalysisRepository.saveAndFlush(SearchCardAnalysis.create(
                userId,
                new AiAnalysisClientResDTO(
                        "WALLET",
                        "CARD_WALLET",
                        List.of(color),
                        null,
                        List.of("LEATHER"),
                        null,
                        List.of("오른쪽 아래 큰 긁힘"),
                        "preprocess-v1"
                )
        ));
    }

    private SearchCardUpdateReqDTO request(Long analysisId) {
        return new SearchCardUpdateReqDTO(
                analysisId,
                "WALLET",
                "남색 카드지갑",
                List.of("NAVY", "BLACK"),
                null,
                "LEATHER",
                "오른쪽 아래에 큰 긁힘이 있어요.",
                LocalDate.of(2026, 8, 17),
                LocalTime.of(18, 0),
                LocalTime.of(21, 0),
                new SearchCardLostLocationReqDTO(
                        "판교역 스타벅스",
                        "경기도 성남시 분당구 판교역로 166",
                        new BigDecimal("37.3947000"),
                        new BigDecimal("127.1112000"),
                        "카페에서 마지막으로 사용했습니다."
                )
        );
    }
}
