package com.dasifind.backend.domain.searchcard.image.service;

import com.dasifind.backend.domain.searchcard.image.entity.SearchCardImageDeletionTask;
import com.dasifind.backend.domain.searchcard.image.repository.SearchCardImageDeletionTaskRepository;
import com.dasifind.backend.domain.searchcard.image.storage.ImageStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SearchCardImageDeletionTaskService {

    private static final Logger log = LoggerFactory.getLogger(
            SearchCardImageDeletionTaskService.class
    );

    private final SearchCardImageDeletionTaskRepository repository;
    private final ImageStorage imageStorage;

    public SearchCardImageDeletionTaskService(
            SearchCardImageDeletionTaskRepository repository,
            ImageStorage imageStorage
    ) {
        this.repository = repository;
        this.imageStorage = imageStorage;
    }

    @Scheduled(
            fixedDelayString = "${app.search-card-image.deletion-cleanup-interval:1m}",
            initialDelayString = "${app.search-card-image.deletion-cleanup-initial-delay:10s}"
    )
    public void deletePendingImages() {
        List<SearchCardImageDeletionTask> tasks = repository.findTop100ByOrderByIdAsc();
        for (SearchCardImageDeletionTask task : tasks) {
            deleteOrKeepForRetry(task);
        }
    }

    private void deleteOrKeepForRetry(SearchCardImageDeletionTask task) {
        try {
            imageStorage.delete(task.getStorageKey());
            repository.delete(task);
        } catch (RuntimeException exception) {
            log.error(
                    "Failed to delete queued search card image taskId={}",
                    task.getId(),
                    exception
            );
        }
    }
}
