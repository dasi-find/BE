package com.dasifind.backend.domain.searchcard.service;

import com.dasifind.backend.domain.searchcard.analysis.client.AiAnalysisClientResponse;
import com.dasifind.backend.domain.searchcard.analysis.entity.SearchCardAnalysis;
import com.dasifind.backend.domain.searchcard.analysis.repository.SearchCardAnalysisRepository;
import com.dasifind.backend.domain.searchcard.entity.LostLocation;
import com.dasifind.backend.domain.searchcard.entity.SearchCard;
import com.dasifind.backend.domain.searchcard.image.entity.SearchCardImage;
import com.dasifind.backend.domain.searchcard.image.model.SearchCardImageType;
import com.dasifind.backend.domain.searchcard.image.repository.SearchCardImageDeletionTaskRepository;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SearchCardDeleteIntegrationTest {

    @Autowired
    private SearchCardDeleteService service;

    @Autowired
    private SearchCardRepository searchCardRepository;

    @Autowired
    private LostLocationRepository lostLocationRepository;

    @Autowired
    private SearchCardAnalysisRepository searchCardAnalysisRepository;

    @Autowired
    private SearchCardImageRepository searchCardImageRepository;

    @Autowired
    private SearchCardImageDeletionTaskRepository imageDeletionTaskRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void 카드와_종속_DB_데이터를_삭제하고_S3_삭제_작업을_남긴다() {
        User user = userRepository.saveAndFlush(User.create(
                "delete-owner@example.com",
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
        LocalDateTime now = LocalDateTime.of(2026, 8, 18, 10, 0);
        SearchCard searchCard = searchCardRepository.saveAndFlush(SearchCard.create(
                user.getId(),
                analysis.getId(),
                "WALLET",
                "남색 카드지갑",
                List.of("NAVY", "BLACK"),
                null,
                "LEATHER",
                "앞면 은색 로고",
                LocalDate.of(2026, 8, 17),
                null,
                null,
                now
        ));
        LostLocation location = lostLocationRepository.saveAndFlush(LostLocation.create(
                searchCard.getId(),
                "판교역",
                "경기도 성남시 분당구 판교역로 166",
                new BigDecimal("37.3947000"),
                new BigDecimal("127.1112000"),
                null,
                now
        ));
        SearchCardImage image = SearchCardImage.create(
                user.getId(),
                "search-card-images/delete/reference.jpg",
                SearchCardImageType.REFERENCE,
                "image/jpeg",
                1024
        );
        image.attachTo(searchCard.getId());
        searchCardImageRepository.saveAndFlush(image);

        service.delete(user.getId(), searchCard.getId());
        entityManager.flush();
        entityManager.clear();

        assertThat(searchCardRepository.findById(searchCard.getId())).isEmpty();
        assertThat(lostLocationRepository.findById(location.getId())).isEmpty();
        assertThat(searchCardImageRepository.findById(image.getId())).isEmpty();
        assertThat(searchCardAnalysisRepository.findById(analysis.getId())).isEmpty();
        assertThat(imageDeletionTaskRepository.findAll())
                .singleElement()
                .extracting(task -> task.getStorageKey())
                .isEqualTo("search-card-images/delete/reference.jpg");
        assertThat(userRepository.findById(user.getId())).isPresent();
    }
}
