package com.dasifind.backend.domain.searchcard.service;

import com.dasifind.backend.domain.searchcard.analysis.client.AiAnalysisClientResDTO;
import com.dasifind.backend.domain.searchcard.analysis.entity.SearchCardAnalysis;
import com.dasifind.backend.domain.searchcard.analysis.repository.SearchCardAnalysisRepository;
import com.dasifind.backend.domain.searchcard.dto.response.SearchCardDetailResDTO;
import com.dasifind.backend.domain.searchcard.entity.LostLocation;
import com.dasifind.backend.domain.searchcard.entity.SearchCard;
import com.dasifind.backend.domain.searchcard.image.entity.SearchCardImage;
import com.dasifind.backend.domain.searchcard.image.model.SearchCardImageType;
import com.dasifind.backend.domain.searchcard.image.repository.SearchCardImageRepository;
import com.dasifind.backend.domain.searchcard.image.storage.ImageStorage;
import com.dasifind.backend.domain.searchcard.repository.LostLocationRepository;
import com.dasifind.backend.domain.searchcard.repository.SearchCardRepository;
import com.dasifind.backend.domain.user.entity.User;
import com.dasifind.backend.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SearchCardDetailQueryIntegrationTest {

    @Autowired
    private SearchCardDetailQueryService service;

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

    @MockitoBean
    private ImageStorage imageStorage;

    @Test
    void 저장된_수색카드의_상세정보와_이미지를_함께_조회한다() {
        User user = userRepository.saveAndFlush(User.create(
                "detail-owner@example.com",
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
        LocalDateTime now = LocalDateTime.of(2026, 8, 18, 10, 0);
        SearchCard searchCard = searchCardRepository.saveAndFlush(SearchCard.create(
                user.getId(),
                analysis.getId(),
                "WALLET",
                "남색 카드지갑",
                List.of("NAVY"),
                null,
                "LEATHER",
                "앞면 중앙에 은색 로고가 있어요.",
                LocalDate.of(2026, 8, 17),
                LocalTime.of(18, 0),
                LocalTime.of(20, 0),
                now
        ));
        lostLocationRepository.saveAndFlush(LostLocation.create(
                searchCard.getId(),
                "판교역 스타벅스",
                "경기도 성남시 분당구 판교역로 166",
                new BigDecimal("37.3947000"),
                new BigDecimal("127.1112000"),
                null,
                now
        ));
        SearchCardImage image = SearchCardImage.create(
                user.getId(),
                "search-card-images/7/reference.jpg",
                SearchCardImageType.REFERENCE,
                "image/jpeg",
                1024
        );
        image.attachTo(searchCard.getId());
        searchCardImageRepository.saveAndFlush(image);
        when(imageStorage.createDownloadUrl(image.getStorageKey()))
                .thenReturn("https://download/reference");

        SearchCardDetailResDTO response = service.getMySearchCard(
                user.getId(),
                searchCard.getId()
        );

        assertThat(response.id()).isEqualTo(searchCard.getId());
        assertThat(response.itemName()).isEqualTo("남색 카드지갑");
        assertThat(response.lostLocation().placeName()).isEqualTo("판교역 스타벅스");
        assertThat(response.analysis().features()).containsExactly("앞면 은색 로고");
        assertThat(response.images()).hasSize(1);
        assertThat(response.images().getFirst().imageUrl())
                .isEqualTo("https://download/reference");
    }
}
