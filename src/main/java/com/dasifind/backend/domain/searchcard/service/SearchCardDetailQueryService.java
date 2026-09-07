package com.dasifind.backend.domain.searchcard.service;

import com.dasifind.backend.domain.searchcard.analysis.entity.SearchCardAnalysis;
import com.dasifind.backend.domain.searchcard.analysis.repository.SearchCardAnalysisRepository;
import com.dasifind.backend.domain.searchcard.dto.response.SearchCardDetailImageResponse;
import com.dasifind.backend.domain.searchcard.dto.response.SearchCardDetailResponse;
import com.dasifind.backend.domain.searchcard.entity.LostLocation;
import com.dasifind.backend.domain.searchcard.entity.SearchCard;
import com.dasifind.backend.domain.searchcard.image.repository.SearchCardImageRepository;
import com.dasifind.backend.domain.searchcard.image.storage.ImageStorage;
import com.dasifind.backend.domain.searchcard.repository.LostLocationRepository;
import com.dasifind.backend.domain.searchcard.repository.SearchCardRepository;
import com.dasifind.backend.domain.user.repository.UserRepository;
import com.dasifind.backend.global.error.BusinessException;
import com.dasifind.backend.global.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class SearchCardDetailQueryService {

    private final SearchCardRepository searchCardRepository;
    private final LostLocationRepository lostLocationRepository;
    private final SearchCardAnalysisRepository searchCardAnalysisRepository;
    private final SearchCardImageRepository searchCardImageRepository;
    private final UserRepository userRepository;
    private final ImageStorage imageStorage;

    public SearchCardDetailQueryService(
            SearchCardRepository searchCardRepository,
            LostLocationRepository lostLocationRepository,
            SearchCardAnalysisRepository searchCardAnalysisRepository,
            SearchCardImageRepository searchCardImageRepository,
            UserRepository userRepository,
            ImageStorage imageStorage
    ) {
        this.searchCardRepository = searchCardRepository;
        this.lostLocationRepository = lostLocationRepository;
        this.searchCardAnalysisRepository = searchCardAnalysisRepository;
        this.searchCardImageRepository = searchCardImageRepository;
        this.userRepository = userRepository;
        this.imageStorage = imageStorage;
    }

    public SearchCardDetailResponse getMySearchCard(Long userId, Long searchCardId) {
        validateUser(userId);
        SearchCard searchCard = searchCardRepository.findById(searchCardId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        validateOwner(userId, searchCard);

        LostLocation lostLocation = lostLocationRepository.findBySearchCardId(searchCardId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        SearchCardAnalysis analysis = searchCardAnalysisRepository.findById(searchCard.getAnalysisId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        List<SearchCardDetailImageResponse> images = searchCardImageRepository
                .findAllBySearchCardIdOrderByIdAsc(searchCardId)
                .stream()
                .map(image -> SearchCardDetailImageResponse.of(
                        image,
                        imageStorage.createDownloadUrl(image.getStorageKey())
                ))
                .toList();

        return SearchCardDetailResponse.of(searchCard, lostLocation, analysis, images);
    }

    private void validateUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
    }

    private void validateOwner(Long userId, SearchCard searchCard) {
        if (!searchCard.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}
