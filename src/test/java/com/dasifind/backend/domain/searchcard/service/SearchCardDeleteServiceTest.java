package com.dasifind.backend.domain.searchcard.service;

import com.dasifind.backend.domain.searchcard.analysis.repository.SearchCardAnalysisRepository;
import com.dasifind.backend.domain.searchcard.entity.SearchCard;
import com.dasifind.backend.domain.searchcard.image.entity.SearchCardImage;
import com.dasifind.backend.domain.searchcard.image.entity.SearchCardImageDeletionTask;
import com.dasifind.backend.domain.searchcard.image.model.SearchCardImageType;
import com.dasifind.backend.domain.searchcard.image.repository.SearchCardImageDeletionTaskRepository;
import com.dasifind.backend.domain.searchcard.image.repository.SearchCardImageRepository;
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
class SearchCardDeleteServiceTest {

    @Mock
    private SearchCardRepository searchCardRepository;

    @Mock
    private SearchCardAnalysisRepository searchCardAnalysisRepository;

    @Mock
    private SearchCardImageRepository searchCardImageRepository;

    @Mock
    private SearchCardImageDeletionTaskRepository imageDeletionTaskRepository;

    @Mock
    private LostLocationRepository lostLocationRepository;

    @Mock
    private UserRepository userRepository;

    private SearchCardDeleteService service;

    @BeforeEach
    void setUp() {
        service = new SearchCardDeleteService(
                searchCardRepository,
                searchCardAnalysisRepository,
                searchCardImageRepository,
                imageDeletionTaskRepository,
                lostLocationRepository,
                userRepository
        );
    }

    @Test
    void 수색카드와_분석_결과를_삭제하고_S3_삭제를_예약한다() {
        SearchCard searchCard = searchCard(12L, 7L, 801L);
        SearchCardImage first = image(501L, "images/first.jpg");
        SearchCardImage second = image(502L, "images/second.jpg");
        when(userRepository.existsById(7L)).thenReturn(true);
        when(searchCardRepository.findByIdForUpdate(12L)).thenReturn(Optional.of(searchCard));
        when(searchCardImageRepository.findAllBySearchCardIdOrderByIdAsc(12L))
                .thenReturn(List.of(first, second));

        service.delete(7L, 12L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SearchCardImageDeletionTask>> tasksCaptor = ArgumentCaptor
                .forClass(List.class);
        verify(imageDeletionTaskRepository).saveAll(tasksCaptor.capture());
        assertThat(tasksCaptor.getValue())
                .extracting(SearchCardImageDeletionTask::getStorageKey)
                .containsExactly("images/first.jpg", "images/second.jpg");
        verify(searchCardRepository).delete(searchCard);
        verify(searchCardRepository).flush();
        verify(searchCardAnalysisRepository).deleteById(801L);
        verify(searchCardAnalysisRepository).flush();
    }

    @Test
    void 이미지가_없어도_수색카드를_삭제한다() {
        SearchCard searchCard = searchCard(12L, 7L, 801L);
        when(userRepository.existsById(7L)).thenReturn(true);
        when(searchCardRepository.findByIdForUpdate(12L)).thenReturn(Optional.of(searchCard));
        when(searchCardImageRepository.findAllBySearchCardIdOrderByIdAsc(12L))
                .thenReturn(List.of());

        service.delete(7L, 12L);

        verify(imageDeletionTaskRepository).saveAll(List.of());
        verify(searchCardRepository).delete(searchCard);
    }

    @Test
    void 다른_사용자의_수색카드는_삭제할_수_없다() {
        when(userRepository.existsById(7L)).thenReturn(true);
        when(searchCardRepository.findByIdForUpdate(12L))
                .thenReturn(Optional.of(searchCard(12L, 8L, 801L)));

        assertError(() -> service.delete(7L, 12L), ErrorCode.FORBIDDEN);

        verify(searchCardImageRepository, never()).findAllBySearchCardIdOrderByIdAsc(12L);
    }

    @Test
    void 존재하지_않는_수색카드는_삭제할_수_없다() {
        when(userRepository.existsById(7L)).thenReturn(true);
        when(searchCardRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

        assertError(() -> service.delete(7L, 999L), ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    void 토큰의_사용자가_없으면_수색카드를_조회하지_않는다() {
        when(userRepository.existsById(7L)).thenReturn(false);

        assertError(() -> service.delete(7L, 12L), ErrorCode.INVALID_TOKEN);

        verify(searchCardRepository, never()).findByIdForUpdate(12L);
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

    private SearchCardImage image(Long id, String storageKey) {
        SearchCardImage image = SearchCardImage.create(
                7L,
                storageKey,
                SearchCardImageType.REFERENCE,
                "image/jpeg",
                1024
        );
        ReflectionTestUtils.setField(image, "id", id);
        image.attachTo(12L);
        return image;
    }
}
