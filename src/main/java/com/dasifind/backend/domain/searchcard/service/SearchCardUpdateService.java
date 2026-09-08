package com.dasifind.backend.domain.searchcard.service;

import com.dasifind.backend.domain.searchcard.analysis.entity.SearchCardAnalysis;
import com.dasifind.backend.domain.searchcard.analysis.repository.SearchCardAnalysisRepository;
import com.dasifind.backend.domain.searchcard.dto.request.SearchCardLostLocationReqDTO;
import com.dasifind.backend.domain.searchcard.dto.request.SearchCardUpdateReqDTO;
import com.dasifind.backend.domain.searchcard.dto.response.SearchCardUpdateResDTO;
import com.dasifind.backend.domain.searchcard.entity.LostLocation;
import com.dasifind.backend.domain.searchcard.entity.SearchCard;
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
public class SearchCardUpdateService {

    private final SearchCardRepository searchCardRepository;
    private final LostLocationRepository lostLocationRepository;
    private final SearchCardAnalysisRepository searchCardAnalysisRepository;
    private final UserRepository userRepository;

    public SearchCardUpdateService(
            SearchCardRepository searchCardRepository,
            LostLocationRepository lostLocationRepository,
            SearchCardAnalysisRepository searchCardAnalysisRepository,
            UserRepository userRepository
    ) {
        this.searchCardRepository = searchCardRepository;
        this.lostLocationRepository = lostLocationRepository;
        this.searchCardAnalysisRepository = searchCardAnalysisRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public SearchCardUpdateResDTO update(
            Long userId,
            Long searchCardId,
            SearchCardUpdateReqDTO request
    ) {
        validateUser(userId);
        validateRequest(request);

        SearchCard searchCard = searchCardRepository.findByIdForUpdate(searchCardId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        validateOwner(userId, searchCard);
        LocalDateTime now = LocalDateTime.now();
        validateStatus(searchCard, now);
        validateAnalysis(userId, request.analysisId());

        LostLocation lostLocation = lostLocationRepository.findBySearchCardId(searchCardId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        Long previousAnalysisId = searchCard.getAnalysisId();
        updateSearchCard(searchCard, request, now);
        updateLostLocation(lostLocation, request.lostLocation(), now);
        searchCardRepository.flush();
        searchCardAnalysisRepository.deleteById(previousAnalysisId);

        return SearchCardUpdateResDTO.from(searchCard);
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

    private void validateStatus(SearchCard searchCard, LocalDateTime now) {
        if (!searchCard.isActiveAt(now)) {
            throw new BusinessException(ErrorCode.INVALID_SEARCH_CARD_STATUS);
        }
    }

    private void validateAnalysis(Long userId, Long analysisId) {
        SearchCardAnalysis analysis = searchCardAnalysisRepository.findByIdForUpdate(analysisId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (!analysis.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (searchCardRepository.existsByAnalysisId(analysisId)) {
            throw new BusinessException(ErrorCode.DUPLICATE_REQUEST);
        }
    }

    private void validateRequest(SearchCardUpdateReqDTO request) {
        if (request.lostStartTime() != null
                && request.lostEndTime() != null
                && request.lostStartTime().isAfter(request.lostEndTime())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        List<String> normalizedColors = request.color().stream()
                .map(String::trim)
                .toList();
        if (new HashSet<>(normalizedColors).size() != normalizedColors.size()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }

    private void updateSearchCard(
            SearchCard searchCard,
            SearchCardUpdateReqDTO request,
            LocalDateTime now
    ) {
        searchCard.update(
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

    private void updateLostLocation(
            LostLocation lostLocation,
            SearchCardLostLocationReqDTO request,
            LocalDateTime now
    ) {
        lostLocation.update(
                request.placeName().trim(),
                request.address().trim(),
                request.latitude(),
                request.longitude(),
                normalizeNullable(request.description()),
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
