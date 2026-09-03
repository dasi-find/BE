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
import com.dasifind.backend.domain.searchcard.image.repository.SearchCardImageRepository;
import com.dasifind.backend.domain.searchcard.image.storage.ImageStorage;
import com.dasifind.backend.domain.user.repository.UserRepository;
import com.dasifind.backend.global.error.BusinessException;
import com.dasifind.backend.global.error.ErrorCode;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;

@Service
public class SearchCardAnalysisService {

    private final SearchCardAnalysisRepository searchCardAnalysisRepository;
    private final SearchCardImageRepository searchCardImageRepository;
    private final UserRepository userRepository;
    private final ImageStorage imageStorage;
    private final AiAnalysisClient aiAnalysisClient;

    public SearchCardAnalysisService(
            SearchCardAnalysisRepository searchCardAnalysisRepository,
            SearchCardImageRepository searchCardImageRepository,
            UserRepository userRepository,
            ImageStorage imageStorage,
            AiAnalysisClient aiAnalysisClient
    ) {
        this.searchCardAnalysisRepository = searchCardAnalysisRepository;
        this.searchCardImageRepository = searchCardImageRepository;
        this.userRepository = userRepository;
        this.imageStorage = imageStorage;
        this.aiAnalysisClient = aiAnalysisClient;
    }

    public SearchCardAnalysisResponse analyze(Long userId, SearchCardAnalysisRequest request) {
        validateUser(userId);
        validateRequest(request);

        List<AiAnalysisClientRequest.Image> images = request.imageIds().stream()
                .map(imageId -> resolveImage(userId, imageId))
                .toList();
        AiAnalysisClientResponse result = aiAnalysisClient.analyze(toClientRequest(request, images));
        validateResult(result);

        SearchCardAnalysis analysis = SearchCardAnalysis.create(userId, result);
        return SearchCardAnalysisResponse.from(searchCardAnalysisRepository.saveAndFlush(analysis));
    }

    private void validateUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
    }

    private void validateRequest(SearchCardAnalysisRequest request) {
        if (request.lostStartTime() != null
                && request.lostEndTime() != null
                && request.lostStartTime().isAfter(request.lostEndTime())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        if (new HashSet<>(request.imageIds()).size() != request.imageIds().size()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }

    private AiAnalysisClientRequest.Image resolveImage(Long userId, Long imageId) {
        SearchCardImage image = searchCardImageRepository.findById(imageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (!image.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return new AiAnalysisClientRequest.Image(
                image.getId(),
                imageStorage.createDownloadUrl(image.getStorageKey()),
                image.getImageType()
        );
    }

    private AiAnalysisClientRequest toClientRequest(
            SearchCardAnalysisRequest request,
            List<AiAnalysisClientRequest.Image> images
    ) {
        LostLocationRequest location = request.lostLocation();
        return new AiAnalysisClientRequest(
                request.category(),
                request.itemName(),
                List.copyOf(request.color()),
                normalizeNullable(request.brand()),
                request.featureDescription(),
                images,
                request.lostDate(),
                request.lostStartTime(),
                request.lostEndTime(),
                new AiAnalysisClientRequest.Location(
                        location.placeName(),
                        location.address(),
                        location.latitude(),
                        location.longitude(),
                        normalizeNullable(location.description())
                )
        );
    }

    private void validateResult(AiAnalysisClientResponse result) {
        if (result == null
                || isInvalidRequired(result.category(), 50)
                || isInvalidRequired(result.itemName(), 100)
                || isInvalidRequired(result.modelVersion(), 100)
                || isInvalidNullable(result.brand(), 100)
                || isInvalidNullable(result.ocrText(), 1000)
                || containsInvalidValue(result.colors(), 10, 50)
                || containsInvalidValue(result.materials(), 10, 50)
                || containsInvalidValue(result.features(), 20, 500)) {
            throw new BusinessException(ErrorCode.AI_ANALYSIS_FAILED);
        }
    }

    private boolean containsInvalidValue(List<String> values, int maxSize, int maxLength) {
        return values == null
                || values.size() > maxSize
                || values.stream().anyMatch(value -> isInvalidRequired(value, maxLength));
    }

    private boolean isInvalidRequired(String value, int maxLength) {
        return value == null || value.isBlank() || value.length() > maxLength;
    }

    private boolean isInvalidNullable(String value, int maxLength) {
        return value != null && value.length() > maxLength;
    }

    private String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
