package com.dasifind.backend.domain.searchcard.image.service;

import com.dasifind.backend.domain.searchcard.image.config.SearchCardImageProperties;
import com.dasifind.backend.domain.searchcard.image.dto.response.SearchCardImageUploadResponse;
import com.dasifind.backend.domain.searchcard.image.entity.SearchCardImage;
import com.dasifind.backend.domain.searchcard.image.model.DetectedImageFormat;
import com.dasifind.backend.domain.searchcard.image.model.SearchCardImageType;
import com.dasifind.backend.domain.searchcard.image.repository.SearchCardImageRepository;
import com.dasifind.backend.domain.searchcard.image.storage.ImageStorage;
import com.dasifind.backend.domain.user.repository.UserRepository;
import com.dasifind.backend.global.error.BusinessException;
import com.dasifind.backend.global.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;

@Service
public class SearchCardImageService {

    private static final String STORAGE_KEY_PREFIX = "search-card-images";

    private final SearchCardImageRepository searchCardImageRepository;
    private final UserRepository userRepository;
    private final ImageStorage imageStorage;
    private final ImageFormatDetector imageFormatDetector;
    private final SearchCardImageProperties properties;

    public SearchCardImageService(
            SearchCardImageRepository searchCardImageRepository,
            UserRepository userRepository,
            ImageStorage imageStorage,
            ImageFormatDetector imageFormatDetector,
            SearchCardImageProperties properties
    ) {
        this.searchCardImageRepository = searchCardImageRepository;
        this.userRepository = userRepository;
        this.imageStorage = imageStorage;
        this.imageFormatDetector = imageFormatDetector;
        this.properties = properties;
    }

    public SearchCardImageUploadResponse upload(
            Long userId,
            MultipartFile file,
            SearchCardImageType imageType
    ) {
        if (!userRepository.existsById(userId)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
        validateFileSize(file);

        byte[] content = readContent(file);
        validateContentSize(content);
        DetectedImageFormat format = imageFormatDetector.detect(content);
        String storageKey = createStorageKey(userId, format.extension());
        String imageUrl = imageStorage.createDownloadUrl(storageKey);

        imageStorage.upload(storageKey, format.contentType(), content);
        SearchCardImage image = SearchCardImage.create(
                userId,
                storageKey,
                imageType,
                format.contentType(),
                content.length
        );
        SearchCardImage savedImage = saveWithCompensation(image, storageKey);
        return SearchCardImageUploadResponse.of(savedImage, imageUrl);
    }

    private void validateFileSize(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_IMAGE_TYPE);
        }
        if (file.getSize() > properties.maxFileSize().toBytes()) {
            throw new BusinessException(ErrorCode.IMAGE_TOO_LARGE);
        }
    }

    private byte[] readContent(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to read uploaded image", exception);
        }
    }

    private void validateContentSize(byte[] content) {
        if (content.length == 0) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_IMAGE_TYPE);
        }
        if (content.length > properties.maxFileSize().toBytes()) {
            throw new BusinessException(ErrorCode.IMAGE_TOO_LARGE);
        }
    }

    private String createStorageKey(Long userId, String extension) {
        return STORAGE_KEY_PREFIX + "/" + userId + "/" + UUID.randomUUID() + "." + extension;
    }

    private SearchCardImage saveWithCompensation(SearchCardImage image, String storageKey) {
        try {
            return searchCardImageRepository.saveAndFlush(image);
        } catch (RuntimeException saveException) {
            try {
                imageStorage.delete(storageKey);
            } catch (RuntimeException deleteException) {
                saveException.addSuppressed(deleteException);
            }
            throw saveException;
        }
    }
}
