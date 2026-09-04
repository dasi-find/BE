package com.dasifind.backend.domain.searchcard.service;

import com.dasifind.backend.domain.searchcard.analysis.entity.SearchCardAnalysis;
import com.dasifind.backend.domain.searchcard.analysis.repository.SearchCardAnalysisRepository;
import com.dasifind.backend.domain.searchcard.dto.request.SearchCardCreateRequest;
import com.dasifind.backend.domain.searchcard.dto.request.SearchCardLostLocationRequest;
import com.dasifind.backend.domain.searchcard.dto.response.SearchCardCreateResponse;
import com.dasifind.backend.domain.searchcard.entity.LostLocation;
import com.dasifind.backend.domain.searchcard.entity.SearchCard;
import com.dasifind.backend.domain.searchcard.image.entity.SearchCardImage;
import com.dasifind.backend.domain.searchcard.image.repository.SearchCardImageRepository;
import com.dasifind.backend.domain.searchcard.repository.LostLocationRepository;
import com.dasifind.backend.domain.searchcard.repository.SearchCardRepository;
import com.dasifind.backend.domain.user.repository.UserRepository;
import com.dasifind.backend.global.error.BusinessException;
import com.dasifind.backend.global.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

@Service
public class SearchCardCreateService {

    private final SearchCardRepository searchCardRepository;
    private final LostLocationRepository lostLocationRepository;
    private final SearchCardAnalysisRepository searchCardAnalysisRepository;
    private final SearchCardImageRepository searchCardImageRepository;
    private final UserRepository userRepository;

    public SearchCardCreateService(
            SearchCardRepository searchCardRepository,
            LostLocationRepository lostLocationRepository,
            SearchCardAnalysisRepository searchCardAnalysisRepository,
            SearchCardImageRepository searchCardImageRepository,
            UserRepository userRepository
    ) {
        this.searchCardRepository = searchCardRepository;
        this.lostLocationRepository = lostLocationRepository;
        this.searchCardAnalysisRepository = searchCardAnalysisRepository;
        this.searchCardImageRepository = searchCardImageRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public SearchCardCreateResponse create(Long userId, SearchCardCreateRequest request) {
        validateUser(userId);
        validateRequest(request);
        validateAnalysis(userId, request.analysisId());

        if (searchCardRepository.existsByAnalysisId(request.analysisId())) {
            throw new BusinessException(ErrorCode.DUPLICATE_REQUEST);
        }

        List<SearchCardImage> images = resolveImages(userId, request.imageIds());
        LocalDateTime now = LocalDateTime.now();
        SearchCard searchCard = searchCardRepository.saveAndFlush(toEntity(userId, request, now));

        SearchCardLostLocationRequest location = request.lostLocation();
        lostLocationRepository.save(LostLocation.create(
                searchCard.getId(),
                location.placeName().trim(),
                location.address().trim(),
                location.latitude(),
                location.longitude(),
                normalizeNullable(location.description()),
                now
        ));
        images.forEach(image -> image.attachTo(searchCard.getId()));

        return SearchCardCreateResponse.from(searchCard);
    }

    private void validateUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
    }

    private void validateAnalysis(Long userId, Long analysisId) {
        SearchCardAnalysis analysis = searchCardAnalysisRepository.findByIdForUpdate(analysisId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (!analysis.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private void validateRequest(SearchCardCreateRequest request) {
        if (request.lostStartTime() != null
                && request.lostEndTime() != null
                && request.lostStartTime().isAfter(request.lostEndTime())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        if (new HashSet<>(request.imageIds()).size() != request.imageIds().size()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        List<String> normalizedColors = request.color().stream()
                .map(String::trim)
                .toList();
        if (new HashSet<>(normalizedColors).size() != normalizedColors.size()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }

    private List<SearchCardImage> resolveImages(Long userId, List<Long> imageIds) {
        if (imageIds.isEmpty()) {
            return List.of();
        }
        List<SearchCardImage> images = searchCardImageRepository.findAllByIdInOrderByIdAsc(imageIds);
        if (images.size() != imageIds.size()) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        if (images.stream().anyMatch(image -> !image.getUserId().equals(userId))) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (images.stream().anyMatch(image -> image.getSearchCardId() != null)) {
            throw new BusinessException(ErrorCode.DUPLICATE_REQUEST);
        }
        return images;
    }

    private SearchCard toEntity(Long userId, SearchCardCreateRequest request, LocalDateTime now) {
        return SearchCard.create(
                userId,
                request.analysisId(),
                request.category().trim(),
                request.itemName().trim(),
                request.color().stream().map(String::trim).toList(),
                normalizeNullable(request.brand()),
                normalizeNullable(request.material()),
                request.featureDescription().trim(),
                request.lostDate(),
                request.lostStartTime(),
                request.lostEndTime(),
                now
        );
    }

    private String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
