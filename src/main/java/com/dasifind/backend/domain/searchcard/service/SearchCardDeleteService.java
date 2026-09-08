package com.dasifind.backend.domain.searchcard.service;

import com.dasifind.backend.domain.searchcard.analysis.repository.SearchCardAnalysisRepository;
import com.dasifind.backend.domain.searchcard.entity.SearchCard;
import com.dasifind.backend.domain.searchcard.image.entity.SearchCardImage;
import com.dasifind.backend.domain.searchcard.image.entity.SearchCardImageDeletionTask;
import com.dasifind.backend.domain.searchcard.image.repository.SearchCardImageDeletionTaskRepository;
import com.dasifind.backend.domain.searchcard.image.repository.SearchCardImageRepository;
import com.dasifind.backend.domain.searchcard.repository.LostLocationRepository;
import com.dasifind.backend.domain.searchcard.repository.SearchCardRepository;
import com.dasifind.backend.domain.user.repository.UserRepository;
import com.dasifind.backend.global.error.BusinessException;
import com.dasifind.backend.global.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SearchCardDeleteService {

    private final SearchCardRepository searchCardRepository;
    private final SearchCardAnalysisRepository searchCardAnalysisRepository;
    private final SearchCardImageRepository searchCardImageRepository;
    private final SearchCardImageDeletionTaskRepository imageDeletionTaskRepository;
    private final LostLocationRepository lostLocationRepository;
    private final UserRepository userRepository;

    public SearchCardDeleteService(
            SearchCardRepository searchCardRepository,
            SearchCardAnalysisRepository searchCardAnalysisRepository,
            SearchCardImageRepository searchCardImageRepository,
            SearchCardImageDeletionTaskRepository imageDeletionTaskRepository,
            LostLocationRepository lostLocationRepository,
            UserRepository userRepository
    ) {
        this.searchCardRepository = searchCardRepository;
        this.searchCardAnalysisRepository = searchCardAnalysisRepository;
        this.searchCardImageRepository = searchCardImageRepository;
        this.imageDeletionTaskRepository = imageDeletionTaskRepository;
        this.lostLocationRepository = lostLocationRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void delete(Long userId, Long searchCardId) {
        validateUser(userId);
        SearchCard searchCard = searchCardRepository.findByIdForUpdate(searchCardId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        validateOwner(userId, searchCard);

        var images = searchCardImageRepository.findAllBySearchCardIdOrderByIdAsc(searchCardId);
        enqueueImageDeletions(images);
        Long analysisId = searchCard.getAnalysisId();
        searchCardImageRepository.deleteAll(images);
        lostLocationRepository.findBySearchCardId(searchCardId)
                .ifPresent(lostLocationRepository::delete);
        searchCardRepository.delete(searchCard);
        searchCardRepository.flush();
        searchCardAnalysisRepository.deleteById(analysisId);
        searchCardAnalysisRepository.flush();
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

    private void enqueueImageDeletions(List<SearchCardImage> images) {
        List<SearchCardImageDeletionTask> tasks = images.stream()
                .map(image -> SearchCardImageDeletionTask.create(image.getStorageKey()))
                .toList();
        imageDeletionTaskRepository.saveAll(tasks);
    }
}
