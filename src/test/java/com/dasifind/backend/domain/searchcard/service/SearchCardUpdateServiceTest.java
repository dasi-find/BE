package com.dasifind.backend.domain.searchcard.service;

import com.dasifind.backend.domain.searchcard.analysis.client.AiAnalysisClientResDTO;
import com.dasifind.backend.domain.searchcard.analysis.entity.SearchCardAnalysis;
import com.dasifind.backend.domain.searchcard.analysis.repository.SearchCardAnalysisRepository;
import com.dasifind.backend.domain.searchcard.dto.request.SearchCardLostLocationReqDTO;
import com.dasifind.backend.domain.searchcard.dto.request.SearchCardUpdateReqDTO;
import com.dasifind.backend.domain.searchcard.dto.response.SearchCardUpdateResDTO;
import com.dasifind.backend.domain.searchcard.entity.LostLocation;
import com.dasifind.backend.domain.searchcard.entity.SearchCard;
import com.dasifind.backend.domain.searchcard.model.SearchCardStatus;
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
class SearchCardUpdateServiceTest {

    @Mock
    private SearchCardRepository searchCardRepository;

    @Mock
    private LostLocationRepository lostLocationRepository;

    @Mock
    private SearchCardAnalysisRepository searchCardAnalysisRepository;

    @Mock
    private UserRepository userRepository;

    private SearchCardUpdateService service;

    @BeforeEach
    void setUp() {
        service = new SearchCardUpdateService(
                searchCardRepository,
                lostLocationRepository,
                searchCardAnalysisRepository,
                userRepository
        );
    }

    @Test
    void 상태가_ACTIVE여도_기간이_지나면_수정할_수_없다() {
        SearchCard card = searchCard(12L, 7L, 801L, SearchCardStatus.ACTIVE);
        ReflectionTestUtils.setField(card, "searchExpiresAt", LocalDateTime.now().minusMinutes(1));
        when(userRepository.existsById(7L)).thenReturn(true);
        when(searchCardRepository.findByIdForUpdate(12L)).thenReturn(Optional.of(card));

        assertError(() -> service.update(7L, 12L, request()), ErrorCode.INVALID_SEARCH_CARD_STATUS);

        verify(searchCardAnalysisRepository, never()).findByIdForUpdate(902L);
        assertThat(card.getAnalysisId()).isEqualTo(801L);
    }

    @Test
    void 새_분석_결과와_전체_수정본으로_수색카드를_수정한다() {
        SearchCard searchCard = searchCard(12L, 7L, 801L, SearchCardStatus.ACTIVE);
        LostLocation location = location(12L);
        when(userRepository.existsById(7L)).thenReturn(true);
        when(searchCardRepository.findByIdForUpdate(12L)).thenReturn(Optional.of(searchCard));
        when(searchCardAnalysisRepository.findByIdForUpdate(902L))
                .thenReturn(Optional.of(analysis(902L, 7L)));
        when(searchCardRepository.existsByAnalysisId(902L)).thenReturn(false);
        when(lostLocationRepository.findBySearchCardId(12L)).thenReturn(Optional.of(location));

        SearchCardUpdateResDTO response = service.update(7L, 12L, request());

        assertThat(response.searchCardId()).isEqualTo(12L);
        assertThat(response.status()).isEqualTo(SearchCardStatus.ACTIVE);
        assertThat(response.rematchScheduled()).isFalse();
        assertThat(searchCard.getAnalysisId()).isEqualTo(902L);
        assertThat(searchCard.getColors()).containsExactly("NAVY", "BLACK");
        assertThat(searchCard.getFeatureDescription()).isEqualTo("오른쪽 아래에 큰 긁힘이 있어요.");
        assertThat(searchCard.getLostEndTime()).isEqualTo(LocalTime.of(21, 0));
        assertThat(location.getPlaceName()).isEqualTo("판교역 스타벅스");
        assertThat(location.getDescription()).isEqualTo("카페에서 마지막으로 사용했습니다.");
    }

    @Test
    void 활성_상태가_아니면_수정할_수_없다() {
        SearchCard searchCard = searchCard(12L, 7L, 801L, SearchCardStatus.CLOSED);
        when(userRepository.existsById(7L)).thenReturn(true);
        when(searchCardRepository.findByIdForUpdate(12L)).thenReturn(Optional.of(searchCard));

        assertError(() -> service.update(7L, 12L, request()), ErrorCode.INVALID_SEARCH_CARD_STATUS);

        verify(searchCardAnalysisRepository, never()).findByIdForUpdate(902L);
    }

    @Test
    void 다른_사용자의_수색카드는_수정할_수_없다() {
        SearchCard searchCard = searchCard(12L, 8L, 801L, SearchCardStatus.ACTIVE);
        when(userRepository.existsById(7L)).thenReturn(true);
        when(searchCardRepository.findByIdForUpdate(12L)).thenReturn(Optional.of(searchCard));

        assertError(() -> service.update(7L, 12L, request()), ErrorCode.FORBIDDEN);
    }

    @Test
    void 존재하지_않는_수색카드는_수정할_수_없다() {
        when(userRepository.existsById(7L)).thenReturn(true);
        when(searchCardRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

        assertError(() -> service.update(7L, 999L, request()), ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    void 다른_사용자의_분석_결과는_사용할_수_없다() {
        SearchCard searchCard = searchCard(12L, 7L, 801L, SearchCardStatus.ACTIVE);
        when(userRepository.existsById(7L)).thenReturn(true);
        when(searchCardRepository.findByIdForUpdate(12L)).thenReturn(Optional.of(searchCard));
        when(searchCardAnalysisRepository.findByIdForUpdate(902L))
                .thenReturn(Optional.of(analysis(902L, 8L)));

        assertError(() -> service.update(7L, 12L, request()), ErrorCode.FORBIDDEN);
    }

    @Test
    void 이미_사용된_분석_결과는_재사용할_수_없다() {
        SearchCard searchCard = searchCard(12L, 7L, 801L, SearchCardStatus.ACTIVE);
        when(userRepository.existsById(7L)).thenReturn(true);
        when(searchCardRepository.findByIdForUpdate(12L)).thenReturn(Optional.of(searchCard));
        when(searchCardAnalysisRepository.findByIdForUpdate(902L))
                .thenReturn(Optional.of(analysis(902L, 7L)));
        when(searchCardRepository.existsByAnalysisId(902L)).thenReturn(true);

        assertError(() -> service.update(7L, 12L, request()), ErrorCode.DUPLICATE_REQUEST);
    }

    @Test
    void 분실_시작시간이_종료시간보다_늦으면_수정할_수_없다() {
        SearchCardUpdateReqDTO invalidRequest = new SearchCardUpdateReqDTO(
                902L,
                "WALLET",
                "남색 카드지갑",
                List.of("NAVY"),
                null,
                "LEATHER",
                "특징",
                LocalDate.of(2026, 8, 17),
                LocalTime.of(22, 0),
                LocalTime.of(21, 0),
                locationRequest()
        );
        when(userRepository.existsById(7L)).thenReturn(true);

        assertError(
                () -> service.update(7L, 12L, invalidRequest),
                ErrorCode.INVALID_REQUEST
        );

        verify(searchCardRepository, never()).findByIdForUpdate(12L);
    }

    @Test
    void 중복_색상이_있으면_수정할_수_없다() {
        SearchCardUpdateReqDTO invalidRequest = new SearchCardUpdateReqDTO(
                902L,
                "WALLET",
                "남색 카드지갑",
                List.of("NAVY", " NAVY "),
                null,
                "LEATHER",
                "특징",
                LocalDate.of(2026, 8, 17),
                null,
                null,
                locationRequest()
        );
        when(userRepository.existsById(7L)).thenReturn(true);

        assertError(
                () -> service.update(7L, 12L, invalidRequest),
                ErrorCode.INVALID_REQUEST
        );
    }

    private void assertError(Runnable action, ErrorCode errorCode) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(errorCode));
    }

    private SearchCardUpdateReqDTO request() {
        return new SearchCardUpdateReqDTO(
                902L,
                " WALLET ",
                " 남색 카드지갑 ",
                List.of(" NAVY ", "BLACK"),
                " ",
                " LEATHER ",
                " 오른쪽 아래에 큰 긁힘이 있어요. ",
                LocalDate.of(2026, 8, 17),
                LocalTime.of(18, 0),
                LocalTime.of(21, 0),
                locationRequest()
        );
    }

    private SearchCardLostLocationReqDTO locationRequest() {
        return new SearchCardLostLocationReqDTO(
                " 판교역 스타벅스 ",
                " 경기도 성남시 분당구 판교역로 166 ",
                new BigDecimal("37.3947000"),
                new BigDecimal("127.1112000"),
                " 카페에서 마지막으로 사용했습니다. "
        );
    }

    private SearchCard searchCard(
            Long id,
            Long userId,
            Long analysisId,
            SearchCardStatus status
    ) {
        SearchCard searchCard = SearchCard.create(
                userId,
                analysisId,
                "WALLET",
                "기존 카드지갑",
                List.of("BLACK"),
                null,
                null,
                "기존 특징",
                LocalDate.of(2026, 8, 16),
                null,
                null,
                LocalDateTime.now()
        );
        ReflectionTestUtils.setField(searchCard, "id", id);
        ReflectionTestUtils.setField(searchCard, "status", status);
        return searchCard;
    }

    private LostLocation location(Long searchCardId) {
        return LostLocation.create(
                searchCardId,
                "판교역",
                "기존 주소",
                new BigDecimal("37.3900000"),
                new BigDecimal("127.1100000"),
                null,
                LocalDateTime.of(2026, 8, 18, 10, 0)
        );
    }

    private SearchCardAnalysis analysis(Long id, Long userId) {
        SearchCardAnalysis analysis = SearchCardAnalysis.create(
                userId,
                new AiAnalysisClientResDTO(
                        "WALLET",
                        "CARD_WALLET",
                        List.of("NAVY"),
                        null,
                        List.of("LEATHER"),
                        null,
                        List.of("오른쪽 아래 큰 긁힘"),
                        "preprocess-v1"
                )
        );
        ReflectionTestUtils.setField(analysis, "id", id);
        return analysis;
    }
}
