package com.dasifind.backend.domain.searchcard.service;

import com.dasifind.backend.domain.searchcard.analysis.client.AiAnalysisClientResponse;
import com.dasifind.backend.domain.searchcard.analysis.entity.SearchCardAnalysis;
import com.dasifind.backend.domain.searchcard.analysis.repository.SearchCardAnalysisRepository;
import com.dasifind.backend.domain.searchcard.dto.request.SearchCardCreateRequest;
import com.dasifind.backend.domain.searchcard.dto.request.SearchCardLostLocationRequest;
import com.dasifind.backend.domain.searchcard.dto.response.SearchCardCreateResponse;
import com.dasifind.backend.domain.searchcard.entity.LostLocation;
import com.dasifind.backend.domain.searchcard.entity.SearchCard;
import com.dasifind.backend.domain.searchcard.image.entity.SearchCardImage;
import com.dasifind.backend.domain.searchcard.image.model.SearchCardImageType;
import com.dasifind.backend.domain.searchcard.image.repository.SearchCardImageRepository;
import com.dasifind.backend.domain.searchcard.model.SearchCardStatus;
import com.dasifind.backend.domain.searchcard.repository.LostLocationRepository;
import com.dasifind.backend.domain.searchcard.repository.SearchCardRepository;
import com.dasifind.backend.domain.user.entity.User;
import com.dasifind.backend.domain.user.repository.UserRepository;
import com.dasifind.backend.global.error.BusinessException;
import com.dasifind.backend.global.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SearchCardCreateServiceIntegrationTest {

    @Autowired
    private SearchCardCreateService searchCardCreateService;

    @Autowired
    private SearchCardRepository searchCardRepository;

    @Autowired
    private LostLocationRepository lostLocationRepository;

    @Autowired
    private SearchCardAnalysisRepository analysisRepository;

    @Autowired
    private SearchCardImageRepository imageRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void 카드와_분실위치를_저장하고_이미지를_연결한다() {
        User user = saveUser("card-create@example.com");
        SearchCardAnalysis analysis = saveAnalysis(user.getId());
        SearchCardImage image = saveImage(user.getId(), "create.png");

        SearchCardCreateResponse response = searchCardCreateService.create(
                user.getId(),
                request(analysis.getId(), List.of(image.getId()))
        );

        SearchCard searchCard = searchCardRepository.findById(response.searchCardId()).orElseThrow();
        LostLocation location = lostLocationRepository
                .findBySearchCardId(searchCard.getId())
                .orElseThrow();
        SearchCardImage attachedImage = imageRepository.findById(image.getId()).orElseThrow();

        assertThat(response.status()).isEqualTo(SearchCardStatus.ACTIVE);
        assertThat(response.initialCandidateCount()).isZero();
        assertThat(response.searchExpiresAt().toLocalTime()).isEqualTo(LocalTime.of(23, 59, 59));
        assertThat(response.searchExpiresAt().toLocalDate())
                .isEqualTo(searchCard.getCreatedAt().toLocalDate().plusDays(30));
        assertThat(searchCard.getColors()).containsExactly("NAVY", "BLACK");
        assertThat(location.getPlaceName()).isEqualTo("판교역");
        assertThat(location.getLatitude()).isEqualByComparingTo("37.3947000");
        assertThat(attachedImage.getSearchCardId()).isEqualTo(searchCard.getId());
    }

    @Test
    void 같은_분석_결과로_수색카드를_중복_생성할_수_없다() {
        User user = saveUser("duplicate-card@example.com");
        SearchCardAnalysis analysis = saveAnalysis(user.getId());
        SearchCardCreateRequest request = request(analysis.getId(), List.of());
        searchCardCreateService.create(user.getId(), request);

        assertThatThrownBy(() -> searchCardCreateService.create(user.getId(), request))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_REQUEST));
    }

    @Test
    void 타인의_분석_결과는_수색카드에_사용할_수_없다() {
        User owner = saveUser("analysis-owner@example.com");
        User requester = saveUser("analysis-requester@example.com");
        SearchCardAnalysis analysis = saveAnalysis(owner.getId());

        assertThatThrownBy(() -> searchCardCreateService.create(
                requester.getId(),
                request(analysis.getId(), List.of())
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    void 이미_다른_카드에_연결된_이미지는_재사용할_수_없다() {
        User user = saveUser("image-reuse@example.com");
        SearchCardAnalysis firstAnalysis = saveAnalysis(user.getId());
        SearchCardAnalysis secondAnalysis = saveAnalysis(user.getId());
        SearchCardImage image = saveImage(user.getId(), "reuse.png");
        searchCardCreateService.create(
                user.getId(),
                request(firstAnalysis.getId(), List.of(image.getId()))
        );

        assertThatThrownBy(() -> searchCardCreateService.create(
                user.getId(),
                request(secondAnalysis.getId(), List.of(image.getId()))
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_REQUEST));
    }

    @Test
    void 종료_시간이_시작_시간보다_빠르면_생성할_수_없다() {
        User user = saveUser("invalid-time@example.com");
        SearchCardAnalysis analysis = saveAnalysis(user.getId());
        SearchCardCreateRequest original = request(analysis.getId(), List.of());
        SearchCardCreateRequest invalid = copyWith(
                original,
                original.imageIds(),
                LocalTime.of(21, 0),
                LocalTime.of(20, 0)
        );

        assertThatThrownBy(() -> searchCardCreateService.create(user.getId(), invalid))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
    }

    @Test
    void 같은_이미지_ID를_중복해서_전달할_수_없다() {
        User user = saveUser("duplicate-image-id@example.com");
        SearchCardAnalysis analysis = saveAnalysis(user.getId());
        SearchCardImage image = saveImage(user.getId(), "duplicate-id.png");
        SearchCardCreateRequest original = request(analysis.getId(), List.of());
        SearchCardCreateRequest invalid = copyWith(
                original,
                List.of(image.getId(), image.getId()),
                original.lostStartTime(),
                original.lostEndTime()
        );

        assertThatThrownBy(() -> searchCardCreateService.create(user.getId(), invalid))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
    }

    @Test
    void 존재하지_않는_이미지는_수색카드에_연결할_수_없다() {
        User user = saveUser("missing-image@example.com");
        SearchCardAnalysis analysis = saveAnalysis(user.getId());

        assertThatThrownBy(() -> searchCardCreateService.create(
                user.getId(),
                request(analysis.getId(), List.of(999999L))
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Test
    void 타인의_이미지는_수색카드에_연결할_수_없다() {
        User owner = saveUser("image-owner@example.com");
        User requester = saveUser("image-requester@example.com");
        SearchCardAnalysis analysis = saveAnalysis(requester.getId());
        SearchCardImage image = saveImage(owner.getId(), "other-user.png");

        assertThatThrownBy(() -> searchCardCreateService.create(
                requester.getId(),
                request(analysis.getId(), List.of(image.getId()))
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }

    private User saveUser(String email) {
        return userRepository.save(User.create(email, "encoded-password", "민준", true));
    }

    private SearchCardAnalysis saveAnalysis(Long userId) {
        return analysisRepository.saveAndFlush(SearchCardAnalysis.create(
                userId,
                new AiAnalysisClientResponse(
                        "WALLET",
                        "CARD_WALLET",
                        List.of("NAVY", "BLACK"),
                        null,
                        List.of("LEATHER"),
                        null,
                        List.of("앞면 은색 로고"),
                        "preprocess-v1"
                )
        ));
    }

    private SearchCardImage saveImage(Long userId, String fileName) {
        return imageRepository.saveAndFlush(SearchCardImage.create(
                userId,
                "search-card-images/" + userId + "/" + fileName,
                SearchCardImageType.ACTUAL,
                "image/png",
                9
        ));
    }

    private SearchCardCreateRequest request(Long analysisId, List<Long> imageIds) {
        return new SearchCardCreateRequest(
                analysisId,
                " WALLET ",
                " 남색 카드지갑 ",
                List.of("NAVY", "BLACK"),
                null,
                "LEATHER",
                " 앞면 중앙에 은색 로고가 있어요. ",
                imageIds,
                LocalDate.of(2026, 8, 17),
                LocalTime.of(18, 0),
                LocalTime.of(20, 0),
                new SearchCardLostLocationRequest(
                        " 판교역 ",
                        " 경기도 성남시 분당구 판교역로 166 ",
                        new BigDecimal("37.3947"),
                        new BigDecimal("127.1112"),
                        null
                )
        );
    }

    private SearchCardCreateRequest copyWith(
            SearchCardCreateRequest original,
            List<Long> imageIds,
            LocalTime lostStartTime,
            LocalTime lostEndTime
    ) {
        return new SearchCardCreateRequest(
                original.analysisId(),
                original.category(),
                original.itemName(),
                new ArrayList<>(original.color()),
                original.brand(),
                original.material(),
                original.featureDescription(),
                imageIds,
                original.lostDate(),
                lostStartTime,
                lostEndTime,
                original.lostLocation()
        );
    }
}
