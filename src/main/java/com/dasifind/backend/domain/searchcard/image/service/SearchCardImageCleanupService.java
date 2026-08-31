package com.dasifind.backend.domain.searchcard.image.service;

import com.dasifind.backend.domain.searchcard.image.config.SearchCardImageProperties;
import com.dasifind.backend.domain.searchcard.image.entity.SearchCardImage;
import com.dasifind.backend.domain.searchcard.image.repository.SearchCardImageRepository;
import com.dasifind.backend.domain.searchcard.image.storage.ImageStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SearchCardImageCleanupService {

    private static final Logger log = LoggerFactory.getLogger(SearchCardImageCleanupService.class);

    private final SearchCardImageRepository searchCardImageRepository;
    private final ImageStorage imageStorage;
    private final SearchCardImageProperties properties;

    public SearchCardImageCleanupService(
            SearchCardImageRepository searchCardImageRepository,
            ImageStorage imageStorage,
            SearchCardImageProperties properties
    ) {
        this.searchCardImageRepository = searchCardImageRepository;
        this.imageStorage = imageStorage;
        this.properties = properties;
    }

    @Scheduled(
            fixedDelayString = "${app.search-card-image.cleanup-interval:1h}",
            initialDelayString = "${app.search-card-image.cleanup-initial-delay:1h}"
    )
    public void cleanupExpiredOrphans() {
        LocalDateTime cutoff = LocalDateTime.now().minus(properties.orphanRetention());
        List<SearchCardImage> expiredImages = searchCardImageRepository
                .findTop100BySearchCardIdIsNullAndCreatedAtBeforeOrderByIdAsc(cutoff);

        for (SearchCardImage image : expiredImages) {
            deleteOrKeepForRetry(image);
        }
    }

    private void deleteOrKeepForRetry(SearchCardImage image) {
        try {
            imageStorage.delete(image.getStorageKey());
            searchCardImageRepository.delete(image);
        } catch (RuntimeException exception) {
            log.error("Failed to clean up orphan search card image id={}", image.getId(), exception);
        }
    }
}
