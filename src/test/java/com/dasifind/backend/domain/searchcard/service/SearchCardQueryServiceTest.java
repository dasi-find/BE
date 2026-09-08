package com.dasifind.backend.domain.searchcard.service;

import com.dasifind.backend.domain.searchcard.dto.response.SearchCardListResDTO;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchCardQueryServiceTest {

    @Mock
    private SearchCardRepository searchCardRepository;

    @Mock
    private LostLocationRepository lostLocationRepository;

    @Mock
    private UserRepository userRepository;

    private SearchCardQueryService service;

    @BeforeEach
    void setUp() {
        service = new SearchCardQueryService(
                searchCardRepository,
                lostLocationRepository,
                userRepository
        );
    }

    @Test
    void 상태를_지정하지_않으면_본인의_모든_수색카드를_조회한다() {
        SearchCard searchCard = searchCard(12L, 7L, "남색 카드지갑");
        LostLocation location = location(12L, "판교역 스타벅스");
        when(userRepository.existsById(7L)).thenReturn(true);
        when(searchCardRepository.findByUserId(any(), any()))
                .thenReturn(new PageImpl<>(List.of(searchCard)));
        when(lostLocationRepository.findAllBySearchCardIdIn(List.of(12L)))
                .thenReturn(List.of(location));

        SearchCardListResDTO response = service.getMySearchCards(7L, null, 0, 20);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().id()).isEqualTo(12L);
        assertThat(response.content().getFirst().itemName()).isEqualTo("남색 카드지갑");
        assertThat(response.content().getFirst().lostPlaceName()).isEqualTo("판교역 스타벅스");
        assertThat(response.content().getFirst().bestCandidateScore()).isNull();

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(searchCardRepository).findByUserId(eq(7L), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(20);
        assertThat(pageable.getSort().getOrderFor("createdAt").isDescending()).isTrue();
        assertThat(pageable.getSort().getOrderFor("id").isDescending()).isTrue();
    }

    @Test
    void 상태를_지정하면_해당_상태의_수색카드만_조회한다() {
        when(userRepository.existsById(7L)).thenReturn(true);
        when(searchCardRepository.findByUserIdAndStatus(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        SearchCardListResDTO response = service.getMySearchCards(
                7L,
                SearchCardStatus.CLOSED,
                1,
                10
        );

        assertThat(response.content()).isEmpty();
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(searchCardRepository).findByUserIdAndStatus(
                eq(7L),
                eq(SearchCardStatus.CLOSED),
                pageableCaptor.capture()
        );
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(1);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(10);
        verify(lostLocationRepository, never()).findAllBySearchCardIdIn(any());
    }

    @Test
    void 토큰의_사용자가_없으면_목록을_조회하지_않는다() {
        when(userRepository.existsById(7L)).thenReturn(false);

        assertError(() -> service.getMySearchCards(7L, null, 0, 20), ErrorCode.INVALID_TOKEN);

        verify(searchCardRepository, never()).findByUserId(any(), any());
    }

    @Test
    void 페이지가_음수이면_조회할_수_없다() {
        when(userRepository.existsById(7L)).thenReturn(true);

        assertError(() -> service.getMySearchCards(7L, null, -1, 20), ErrorCode.INVALID_REQUEST);
    }

    @Test
    void 페이지_크기는_1에서_100_사이여야_한다() {
        when(userRepository.existsById(7L)).thenReturn(true);

        assertError(() -> service.getMySearchCards(7L, null, 0, 0), ErrorCode.INVALID_REQUEST);
        assertError(() -> service.getMySearchCards(7L, null, 0, 101), ErrorCode.INVALID_REQUEST);
    }

    private void assertError(Runnable action, ErrorCode expectedErrorCode) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(expectedErrorCode));
    }

    private SearchCard searchCard(Long id, Long userId, String itemName) {
        SearchCard searchCard = SearchCard.create(
                userId,
                801L,
                "WALLET",
                itemName,
                List.of("NAVY"),
                null,
                "LEATHER",
                "앞면 은색 로고",
                LocalDate.of(2026, 8, 17),
                null,
                null,
                LocalDateTime.of(2026, 8, 18, 10, 0)
        );
        ReflectionTestUtils.setField(searchCard, "id", id);
        return searchCard;
    }

    private LostLocation location(Long searchCardId, String placeName) {
        return LostLocation.create(
                searchCardId,
                placeName,
                "경기도 성남시 분당구 판교역로 166",
                new BigDecimal("37.3947000"),
                new BigDecimal("127.1112000"),
                null,
                LocalDateTime.of(2026, 8, 18, 10, 0)
        );
    }
}
