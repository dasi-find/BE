package com.dasifind.backend.domain.searchcard.service;

import com.dasifind.backend.domain.searchcard.analysis.client.AiAnalysisClientResponse;
import com.dasifind.backend.domain.searchcard.analysis.entity.SearchCardAnalysis;
import com.dasifind.backend.domain.searchcard.analysis.repository.SearchCardAnalysisRepository;
import com.dasifind.backend.domain.searchcard.dto.response.SearchCardDetailResponse;
import com.dasifind.backend.domain.searchcard.entity.LostLocation;
import com.dasifind.backend.domain.searchcard.entity.SearchCard;
import com.dasifind.backend.domain.searchcard.image.entity.SearchCardImage;
import com.dasifind.backend.domain.searchcard.image.model.SearchCardImageType;
import com.dasifind.backend.domain.searchcard.image.repository.SearchCardImageRepository;
import com.dasifind.backend.domain.searchcard.image.storage.ImageStorage;
import com.dasifind.backend.domain.searchcard.repository.LostLocationRepository;
import com.dasifind.backend.domain.searchcard.repository.SearchCardRepository;
import com.dasifind.backend.domain.user.repository.UserRepository;
import com.dasifind.backend.global.error.BusinessException;
import com.dasifind.backend.global.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchCardDetailQueryServiceTest {

    @Mock
    private SearchCardRepository searchCardRepository;

    @Mock
    private LostLocationRepository lostLocationRepository;

    @Mock
    private SearchCardAnalysisRepository searchCardAnalysisRepository;

    @Mock
    private SearchCardImageRepository searchCardImageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ImageStorage imageStorage;

    private SearchCardDetailQueryService service;

    @BeforeEach
    void setUp() {
        service = new SearchCardDetailQueryService(
                searchCardRepository,
                lostLocationRepository,
                searchCardAnalysisRepository,
                searchCardImageRepository,
                userRepository,
                imageStorage
        );
    }

    @Test
    void 본인의_수색카드_상세정보를_조회한다() {
        SearchCard searchCard = searchCard(12L, 7L, 801L);
        LostLocation location = location(12L);
        SearchCardAnalysis analysis = analysis(801L, 7L);
        SearchCardImage firstImage = image(501L, 7L, 12L, "first.jpg");
        SearchCardImage secondImage = image(502L, 7L, 12L, "second.jpg");

        when(userRepository.existsById(7L)).thenReturn(true);
        when(searchCardRepository.findById(12L)).thenReturn(Optional.of(searchCard));
        when(lostLocationRepository.findBySearchCardId(12L)).thenReturn(Optional.of(location));
        when(searchCardAnalysisRepository.findById(801L)).thenReturn(Optional.of(analysis));
        when(searchCardImageRepository.findAllBySearchCardIdOrderByIdAsc(12L))
                .thenReturn(List.of(firstImage, secondImage));
        when(imageStorage.createDownloadUrl("first.jpg")).thenReturn("https://download/first");
        when(imageStorage.createDownloadUrl("second.jpg")).thenReturn("https://download/second");

        SearchCardDetailResponse response = service.getMySearchCard(7L, 12L);

        assertThat(response.id()).isEqualTo(12L);
        assertThat(response.category()).isEqualTo("WALLET");
        assertThat(response.colors()).containsExactly("NAVY", "BLACK");
        assertThat(response.lostLocation().placeName()).isEqualTo("판교역 스타벅스");
        assertThat(response.analysis().features()).containsExactly("앞면 은색 로고");
        assertThat(response.images()).extracting(imageResponse -> imageResponse.id())
                .containsExactly(501L, 502L);
        assertThat(response.images()).extracting(imageResponse -> imageResponse.imageUrl())
                .containsExactly("https://download/first", "https://download/second");
        assertThat(response.candidateCount()).isZero();
        assertThat(response.bestCandidateScore()).isNull();
    }

    @Test
    void 연결된_이미지가_없으면_빈_목록을_반환한다() {
        SearchCard searchCard = searchCard(12L, 7L, 801L);
        when(userRepository.existsById(7L)).thenReturn(true);
        when(searchCardRepository.findById(12L)).thenReturn(Optional.of(searchCard));
        when(lostLocationRepository.findBySearchCardId(12L))
                .thenReturn(Optional.of(location(12L)));
        when(searchCardAnalysisRepository.findById(801L))
                .thenReturn(Optional.of(analysis(801L, 7L)));
        when(searchCardImageRepository.findAllBySearchCardIdOrderByIdAsc(12L))
                .thenReturn(List.of());

        SearchCardDetailResponse response = service.getMySearchCard(7L, 12L);

        assertThat(response.images()).isEmpty();
        verify(imageStorage, never()).createDownloadUrl("unused");
    }

    @Test
    void 토큰의_사용자가_없으면_상세를_조회하지_않는다() {
        when(userRepository.existsById(7L)).thenReturn(false);

        assertError(
                () -> service.getMySearchCard(7L, 12L),
                ErrorCode.INVALID_TOKEN
        );

        verify(searchCardRepository, never()).findById(12L);
    }

    @Test
    void 수색카드가_없으면_리소스_없음_오류가_발생한다() {
        when(userRepository.existsById(7L)).thenReturn(true);
        when(searchCardRepository.findById(999L)).thenReturn(Optional.empty());

        assertError(
                () -> service.getMySearchCard(7L, 999L),
                ErrorCode.RESOURCE_NOT_FOUND
        );
    }

    @Test
    void 다른_사용자의_수색카드이면_접근할_수_없다() {
        when(userRepository.existsById(7L)).thenReturn(true);
        when(searchCardRepository.findById(12L))
                .thenReturn(Optional.of(searchCard(12L, 8L, 801L)));

        assertError(
                () -> service.getMySearchCard(7L, 12L),
                ErrorCode.FORBIDDEN
        );

        verify(lostLocationRepository, never()).findBySearchCardId(12L);
    }

    @Test
    void 분실위치가_없으면_불완전한_리소스로_처리한다() {
        SearchCard searchCard = searchCard(12L, 7L, 801L);
        when(userRepository.existsById(7L)).thenReturn(true);
        when(searchCardRepository.findById(12L)).thenReturn(Optional.of(searchCard));
        when(lostLocationRepository.findBySearchCardId(12L)).thenReturn(Optional.empty());

        assertError(
                () -> service.getMySearchCard(7L, 12L),
                ErrorCode.RESOURCE_NOT_FOUND
        );
    }

    private void assertError(Runnable action, ErrorCode errorCode) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(errorCode));
    }

    private SearchCard searchCard(Long id, Long userId, Long analysisId) {
        SearchCard searchCard = SearchCard.create(
                userId,
                analysisId,
                "WALLET",
                "남색 카드지갑",
                List.of("NAVY", "BLACK"),
                null,
                "LEATHER",
                "앞면 중앙에 은색 로고가 있어요.",
                LocalDate.of(2026, 8, 17),
                LocalTime.of(18, 0),
                LocalTime.of(20, 0),
                LocalDateTime.of(2026, 8, 18, 10, 0)
        );
        ReflectionTestUtils.setField(searchCard, "id", id);
        return searchCard;
    }

    private LostLocation location(Long searchCardId) {
        return LostLocation.create(
                searchCardId,
                "판교역 스타벅스",
                "경기도 성남시 분당구 판교역로 166",
                new BigDecimal("37.3947000"),
                new BigDecimal("127.1112000"),
                "카페에서 나올 때까지는 있었어요.",
                LocalDateTime.of(2026, 8, 18, 10, 0)
        );
    }

    private SearchCardAnalysis analysis(Long id, Long userId) {
        SearchCardAnalysis analysis = SearchCardAnalysis.create(
                userId,
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
        );
        ReflectionTestUtils.setField(analysis, "id", id);
        return analysis;
    }

    private SearchCardImage image(
            Long id,
            Long userId,
            Long searchCardId,
            String storageKey
    ) {
        SearchCardImage image = SearchCardImage.create(
                userId,
                storageKey,
                SearchCardImageType.REFERENCE,
                "image/jpeg",
                1024
        );
        ReflectionTestUtils.setField(image, "id", id);
        image.attachTo(searchCardId);
        return image;
    }
}
