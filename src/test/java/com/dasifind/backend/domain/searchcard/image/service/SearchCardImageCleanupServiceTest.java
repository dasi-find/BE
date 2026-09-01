package com.dasifind.backend.domain.searchcard.image.service;

import com.dasifind.backend.domain.searchcard.image.config.SearchCardImageProperties;
import com.dasifind.backend.domain.searchcard.image.entity.SearchCardImage;
import com.dasifind.backend.domain.searchcard.image.model.SearchCardImageType;
import com.dasifind.backend.domain.searchcard.image.repository.SearchCardImageRepository;
import com.dasifind.backend.domain.searchcard.image.storage.ImageStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.unit.DataSize;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchCardImageCleanupServiceTest {

    @Mock
    private SearchCardImageRepository repository;

    @Mock
    private ImageStorage imageStorage;

    @Test
    void 생성된지_24시간이_지난_미연결_이미지를_S3와_DB에서_삭제한다() {
        SearchCardImage image = image(501L, "search-card-images/7/image.png");
        when(repository.findTop100BySearchCardIdIsNullAndCreatedAtBeforeOrderByIdAsc(any()))
                .thenReturn(List.of(image));
        SearchCardImageCleanupService service = service();

        service.cleanupExpiredOrphans();

        verify(imageStorage).delete("search-card-images/7/image.png");
        verify(repository).delete(image);
    }

    @Test
    void S3_삭제에_실패하면_DB_기록을_남겨_다음_정리에서_재시도한다() {
        SearchCardImage image = image(501L, "search-card-images/7/image.png");
        when(repository.findTop100BySearchCardIdIsNullAndCreatedAtBeforeOrderByIdAsc(any()))
                .thenReturn(List.of(image));
        doThrow(new RuntimeException("S3 unavailable"))
                .when(imageStorage).delete("search-card-images/7/image.png");

        service().cleanupExpiredOrphans();

        verify(repository, never()).delete(image);
    }

    private SearchCardImageCleanupService service() {
        return new SearchCardImageCleanupService(
                repository,
                imageStorage,
                new SearchCardImageProperties(DataSize.ofMegabytes(10), Duration.ofHours(24))
        );
    }

    private SearchCardImage image(Long id, String storageKey) {
        SearchCardImage image = SearchCardImage.create(
                7L,
                storageKey,
                SearchCardImageType.ACTUAL,
                "image/png",
                9
        );
        ReflectionTestUtils.setField(image, "id", id);
        ReflectionTestUtils.setField(image, "createdAt", LocalDateTime.now().minusHours(25));
        return image;
    }
}
