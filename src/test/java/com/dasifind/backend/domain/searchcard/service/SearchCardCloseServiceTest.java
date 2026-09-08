package com.dasifind.backend.domain.searchcard.service;

import com.dasifind.backend.domain.searchcard.dto.request.SearchCardCloseReqDTO;
import com.dasifind.backend.domain.searchcard.dto.response.SearchCardCloseResDTO;
import com.dasifind.backend.domain.searchcard.entity.SearchCard;
import com.dasifind.backend.domain.searchcard.model.SearchCardCloseReason;
import com.dasifind.backend.domain.searchcard.model.SearchCardStatus;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchCardCloseServiceTest {

    @Mock
    private SearchCardRepository searchCardRepository;

    @Mock
    private UserRepository userRepository;

    private SearchCardCloseService service;

    @BeforeEach
    void setUp() {
        service = new SearchCardCloseService(searchCardRepository, userRepository);
    }

    @Test
    void 상태가_ACTIVE여도_기간이_지났으면_종료할_수_없다() {
        SearchCard card = searchCard(12L, 7L, SearchCardStatus.ACTIVE);
        ReflectionTestUtils.setField(card, "searchExpiresAt", LocalDateTime.now().minusMinutes(1));
        givenOwnedCard(card);

        assertError(() -> service.close(7L, 12L,
                request(SearchCardStatus.FOUND, SearchCardCloseReason.FOUND_OTHER_WAY)),
                ErrorCode.INVALID_SEARCH_CARD_STATUS);
        assertThat(card.getStatus()).isEqualTo(SearchCardStatus.ACTIVE);
        assertThat(card.getClosedAt()).isNull();
    }

    @Test
    void 추천_후보로_찾은_활성_수색카드를_FOUND로_종료한다() {
        SearchCard searchCard = searchCard(12L, 7L, SearchCardStatus.ACTIVE);
        givenOwnedCard(searchCard);

        SearchCardCloseResDTO response = service.close(
                7L,
                12L,
                request(SearchCardStatus.FOUND, SearchCardCloseReason.FOUND_BY_RECOMMENDATION)
        );

        assertThat(response.searchCardId()).isEqualTo(12L);
        assertThat(response.status()).isEqualTo(SearchCardStatus.FOUND);
        assertThat(response.closedAt()).isNotNull();
        assertThat(searchCard.getCloseReason())
                .isEqualTo(SearchCardCloseReason.FOUND_BY_RECOMMENDATION);
        assertThat(searchCard.getClosedAt()).isEqualTo(response.closedAt());
    }

    @Test
    void 다른_방법으로_찾은_활성_수색카드를_FOUND로_종료한다() {
        SearchCard searchCard = searchCard(12L, 7L, SearchCardStatus.ACTIVE);
        givenOwnedCard(searchCard);

        service.close(
                7L,
                12L,
                request(SearchCardStatus.FOUND, SearchCardCloseReason.FOUND_OTHER_WAY)
        );

        assertThat(searchCard.getStatus()).isEqualTo(SearchCardStatus.FOUND);
        assertThat(searchCard.getCloseReason()).isEqualTo(SearchCardCloseReason.FOUND_OTHER_WAY);
    }

    @Test
    void 찾지_못하고_수색을_중단하면_CLOSED로_종료한다() {
        SearchCard searchCard = searchCard(12L, 7L, SearchCardStatus.ACTIVE);
        givenOwnedCard(searchCard);

        service.close(
                7L,
                12L,
                request(SearchCardStatus.CLOSED, SearchCardCloseReason.SEARCH_STOPPED)
        );

        assertThat(searchCard.getStatus()).isEqualTo(SearchCardStatus.CLOSED);
        assertThat(searchCard.getCloseReason()).isEqualTo(SearchCardCloseReason.SEARCH_STOPPED);
    }

    @Test
    void 종료_상태와_사유의_조합이_맞지_않으면_거절한다() {
        SearchCard foundMismatch = searchCard(12L, 7L, SearchCardStatus.ACTIVE);
        givenOwnedCard(foundMismatch);

        assertError(
                () -> service.close(
                        7L,
                        12L,
                        request(SearchCardStatus.FOUND, SearchCardCloseReason.SEARCH_STOPPED)
                ),
                ErrorCode.INVALID_REQUEST
        );

        SearchCard closedMismatch = searchCard(13L, 7L, SearchCardStatus.ACTIVE);
        when(searchCardRepository.findByIdForUpdate(13L)).thenReturn(Optional.of(closedMismatch));
        assertError(
                () -> service.close(
                        7L,
                        13L,
                        request(SearchCardStatus.CLOSED, SearchCardCloseReason.FOUND_OTHER_WAY)
                ),
                ErrorCode.INVALID_REQUEST
        );
    }

    @Test
    void ACTIVE가_아닌_수색카드는_다시_종료할_수_없다() {
        SearchCard searchCard = searchCard(12L, 7L, SearchCardStatus.FOUND);
        givenOwnedCard(searchCard);

        assertError(
                () -> service.close(
                        7L,
                        12L,
                        request(SearchCardStatus.CLOSED, SearchCardCloseReason.SEARCH_STOPPED)
                ),
                ErrorCode.INVALID_SEARCH_CARD_STATUS
        );
    }

    @Test
    void 다른_사용자의_수색카드는_종료할_수_없다() {
        SearchCard searchCard = searchCard(12L, 8L, SearchCardStatus.ACTIVE);
        when(userRepository.existsById(7L)).thenReturn(true);
        when(searchCardRepository.findByIdForUpdate(12L)).thenReturn(Optional.of(searchCard));

        assertError(
                () -> service.close(
                        7L,
                        12L,
                        request(SearchCardStatus.CLOSED, SearchCardCloseReason.SEARCH_STOPPED)
                ),
                ErrorCode.FORBIDDEN
        );
    }

    @Test
    void 존재하지_않는_수색카드는_종료할_수_없다() {
        when(userRepository.existsById(7L)).thenReturn(true);
        when(searchCardRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

        assertError(
                () -> service.close(
                        7L,
                        999L,
                        request(SearchCardStatus.CLOSED, SearchCardCloseReason.SEARCH_STOPPED)
                ),
                ErrorCode.RESOURCE_NOT_FOUND
        );
    }

    @Test
    void 토큰의_사용자가_없으면_수색카드를_조회하지_않는다() {
        when(userRepository.existsById(7L)).thenReturn(false);

        assertError(
                () -> service.close(
                        7L,
                        12L,
                        request(SearchCardStatus.CLOSED, SearchCardCloseReason.SEARCH_STOPPED)
                ),
                ErrorCode.INVALID_TOKEN
        );

        verify(searchCardRepository, never()).findByIdForUpdate(12L);
    }

    private void givenOwnedCard(SearchCard searchCard) {
        when(userRepository.existsById(7L)).thenReturn(true);
        when(searchCardRepository.findByIdForUpdate(searchCard.getId()))
                .thenReturn(Optional.of(searchCard));
    }

    private SearchCardCloseReqDTO request(
            SearchCardStatus status,
            SearchCardCloseReason reason
    ) {
        return new SearchCardCloseReqDTO(status, reason);
    }

    private void assertError(Runnable action, ErrorCode errorCode) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(errorCode));
    }

    private SearchCard searchCard(
            Long id,
            Long userId,
            SearchCardStatus status
    ) {
        SearchCard searchCard = SearchCard.create(
                userId,
                801L,
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
        );
        ReflectionTestUtils.setField(searchCard, "id", id);
        ReflectionTestUtils.setField(searchCard, "status", status);
        return searchCard;
    }
}
