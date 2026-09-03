package com.dasifind.backend.domain.searchcard.analysis.service;

import com.dasifind.backend.domain.searchcard.analysis.client.AiAnalysisClient;
import com.dasifind.backend.domain.searchcard.analysis.client.AiAnalysisClientRequest;
import com.dasifind.backend.domain.searchcard.analysis.client.AiAnalysisClientResponse;
import com.dasifind.backend.domain.searchcard.analysis.dto.request.LostLocationRequest;
import com.dasifind.backend.domain.searchcard.analysis.dto.request.SearchCardAnalysisRequest;
import com.dasifind.backend.domain.searchcard.analysis.dto.response.SearchCardAnalysisResponse;
import com.dasifind.backend.domain.searchcard.analysis.entity.SearchCardAnalysis;
import com.dasifind.backend.domain.searchcard.analysis.repository.SearchCardAnalysisRepository;
import com.dasifind.backend.domain.searchcard.image.entity.SearchCardImage;
import com.dasifind.backend.domain.searchcard.image.model.SearchCardImageType;
import com.dasifind.backend.domain.searchcard.image.repository.SearchCardImageRepository;
import com.dasifind.backend.domain.searchcard.image.storage.ImageStorage;
import com.dasifind.backend.domain.user.repository.UserRepository;
import com.dasifind.backend.global.error.BusinessException;
import com.dasifind.backend.global.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchCardAnalysisServiceTest {

    @Mock
    private SearchCardAnalysisRepository analysisRepository;
    @Mock
    private SearchCardImageRepository imageRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ImageStorage imageStorage;
    @Mock
    private AiAnalysisClient aiAnalysisClient;

    private SearchCardAnalysisService service;

    @BeforeEach
    void setUp() {
        service = new SearchCardAnalysisService(
                analysisRepository,
                imageRepository,
                userRepository,
                imageStorage,
                aiAnalysisClient
        );
    }

    @Test
    void 사진_없이_텍스트만_AI로_전달하고_결과를_저장한다() {
        SearchCardAnalysisRequest request = request(List.of());
        AiAnalysisClientResponse aiResult = aiResult();
        when(userRepository.existsById(7L)).thenReturn(true);
        when(aiAnalysisClient.analyze(any())).thenReturn(aiResult);
        when(analysisRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            SearchCardAnalysis analysis = invocation.getArgument(0);
            ReflectionTestUtils.setField(analysis, "id", 801L);
            return analysis;
        });

        SearchCardAnalysisResponse response = service.analyze(7L, request);

        ArgumentCaptor<AiAnalysisClientRequest> clientRequestCaptor =
                ArgumentCaptor.forClass(AiAnalysisClientRequest.class);
        verify(aiAnalysisClient).analyze(clientRequestCaptor.capture());
        assertThat(clientRequestCaptor.getValue().images()).isEmpty();
        assertThat(response.analysisId()).isEqualTo(801L);
        assertThat(response.itemName()).isEqualTo("CARD_WALLET");
        assertThat(response.colors()).containsExactly("NAVY", "BLACK");
        verify(analysisRepository).saveAndFlush(any(SearchCardAnalysis.class));
    }

    @Test
    void 본인_이미지의_Presigned_URL을_AI로_전달한다() {
        SearchCardImage image = SearchCardImage.create(
                7L,
                "search-card-images/7/wallet.png",
                SearchCardImageType.ACTUAL,
                "image/png",
                9
        );
        ReflectionTestUtils.setField(image, "id", 501L);
        when(userRepository.existsById(7L)).thenReturn(true);
        when(imageRepository.findById(501L)).thenReturn(Optional.of(image));
        when(imageStorage.createDownloadUrl(image.getStorageKey()))
                .thenReturn("https://presigned.example/wallet.png");
        when(aiAnalysisClient.analyze(any())).thenReturn(aiResult());
        when(analysisRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.analyze(7L, request(List.of(501L)));

        ArgumentCaptor<AiAnalysisClientRequest> requestCaptor =
                ArgumentCaptor.forClass(AiAnalysisClientRequest.class);
        verify(aiAnalysisClient).analyze(requestCaptor.capture());
        assertThat(requestCaptor.getValue().images()).containsExactly(
                new AiAnalysisClientRequest.Image(
                        501L,
                        "https://presigned.example/wallet.png",
                        SearchCardImageType.ACTUAL
                )
        );
    }

    @Test
    void 타인_이미지는_AI로_전달하지_않는다() {
        SearchCardImage image = SearchCardImage.create(
                8L,
                "search-card-images/8/wallet.png",
                SearchCardImageType.ACTUAL,
                "image/png",
                9
        );
        when(userRepository.existsById(7L)).thenReturn(true);
        when(imageRepository.findById(501L)).thenReturn(Optional.of(image));

        assertError(request(List.of(501L)), ErrorCode.FORBIDDEN);

        verify(aiAnalysisClient, never()).analyze(any());
    }

    @Test
    void 존재하지_않는_이미지는_AI로_전달하지_않는다() {
        when(userRepository.existsById(7L)).thenReturn(true);
        when(imageRepository.findById(501L)).thenReturn(Optional.empty());

        assertError(request(List.of(501L)), ErrorCode.RESOURCE_NOT_FOUND);

        verify(aiAnalysisClient, never()).analyze(any());
    }

    @Test
    void 중복된_이미지_ID는_거절한다() {
        when(userRepository.existsById(7L)).thenReturn(true);

        assertError(request(List.of(501L, 501L)), ErrorCode.INVALID_REQUEST);

        verify(imageRepository, never()).findById(any());
        verify(aiAnalysisClient, never()).analyze(any());
    }

    @Test
    void 종료_시간이_시작_시간보다_빠르면_거절한다() {
        when(userRepository.existsById(7L)).thenReturn(true);
        SearchCardAnalysisRequest original = request(List.of());
        SearchCardAnalysisRequest invalid = new SearchCardAnalysisRequest(
                original.category(), original.itemName(), original.color(), original.brand(),
                original.featureDescription(), original.imageIds(), original.lostDate(),
                LocalTime.of(20, 0), LocalTime.of(18, 0), original.lostLocation()
        );

        assertError(invalid, ErrorCode.INVALID_REQUEST);

        verify(aiAnalysisClient, never()).analyze(any());
    }

    @Test
    void AI가_불완전한_결과를_반환하면_저장하지_않는다() {
        when(userRepository.existsById(7L)).thenReturn(true);
        when(aiAnalysisClient.analyze(any())).thenReturn(new AiAnalysisClientResponse(
                "WALLET", "CARD_WALLET", null, null, List.of(), null, List.of(), "v1"
        ));

        assertError(request(List.of()), ErrorCode.AI_ANALYSIS_FAILED);

        verify(analysisRepository, never()).saveAndFlush(any());
    }

    @Test
    void 토큰의_사용자가_없으면_AI를_호출하지_않는다() {
        when(userRepository.existsById(7L)).thenReturn(false);

        assertError(request(List.of()), ErrorCode.INVALID_TOKEN);

        verify(aiAnalysisClient, never()).analyze(any());
        verify(imageStorage, never()).createDownloadUrl(anyString());
    }

    private void assertError(SearchCardAnalysisRequest request, ErrorCode expectedErrorCode) {
        assertThatThrownBy(() -> service.analyze(7L, request))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(expectedErrorCode));
    }

    private SearchCardAnalysisRequest request(List<Long> imageIds) {
        return new SearchCardAnalysisRequest(
                "WALLET",
                "남색 카드지갑",
                List.of("NAVY"),
                null,
                "앞면 중앙에 은색 로고가 있어요.",
                imageIds,
                LocalDate.of(2026, 8, 17),
                LocalTime.of(18, 0),
                LocalTime.of(20, 0),
                new LostLocationRequest(
                        "판교역",
                        "경기도 성남시 분당구 판교역로 166",
                        new BigDecimal("37.3947"),
                        new BigDecimal("127.1112"),
                        null
                )
        );
    }

    private AiAnalysisClientResponse aiResult() {
        return new AiAnalysisClientResponse(
                "WALLET",
                "CARD_WALLET",
                List.of("NAVY", "BLACK"),
                null,
                List.of("LEATHER"),
                null,
                List.of("앞면 은색 로고"),
                "preprocess-v1"
        );
    }
}
