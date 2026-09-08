package com.dasifind.backend.domain.searchcard.image.service;

import com.dasifind.backend.domain.searchcard.image.entity.SearchCardImageDeletionTask;
import com.dasifind.backend.domain.searchcard.image.repository.SearchCardImageDeletionTaskRepository;
import com.dasifind.backend.domain.searchcard.image.storage.ImageStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchCardImageDeletionTaskServiceTest {

    @Mock
    private SearchCardImageDeletionTaskRepository repository;

    @Mock
    private ImageStorage imageStorage;

    private SearchCardImageDeletionTaskService service;

    @BeforeEach
    void setUp() {
        service = new SearchCardImageDeletionTaskService(repository, imageStorage);
    }

    @Test
    void S3_이미지_삭제에_성공하면_작업을_삭제한다() {
        SearchCardImageDeletionTask task = task(1L, "images/first.jpg");
        when(repository.findTop100ByOrderByIdAsc()).thenReturn(List.of(task));

        service.deletePendingImages();

        verify(imageStorage).delete("images/first.jpg");
        verify(repository).delete(task);
    }

    @Test
    void S3_이미지_삭제에_실패하면_작업을_남겨_재시도한다() {
        SearchCardImageDeletionTask task = task(1L, "images/first.jpg");
        when(repository.findTop100ByOrderByIdAsc()).thenReturn(List.of(task));
        doThrow(new RuntimeException("S3 unavailable"))
                .when(imageStorage).delete("images/first.jpg");

        service.deletePendingImages();

        verify(repository, never()).delete(task);
    }

    private SearchCardImageDeletionTask task(Long id, String storageKey) {
        SearchCardImageDeletionTask task = SearchCardImageDeletionTask.create(storageKey);
        ReflectionTestUtils.setField(task, "id", id);
        return task;
    }
}
