package com.dasifind.backend.domain.searchcard.service;

import com.dasifind.backend.domain.searchcard.dto.request.SearchCardCloseReqDTO;
import com.dasifind.backend.domain.searchcard.dto.response.SearchCardCloseResDTO;
import com.dasifind.backend.domain.searchcard.entity.SearchCard;
import com.dasifind.backend.domain.searchcard.model.SearchCardCloseReason;
import com.dasifind.backend.domain.searchcard.model.SearchCardStatus;
import com.dasifind.backend.domain.searchcard.repository.SearchCardRepository;
import com.dasifind.backend.domain.user.repository.UserRepository;
import com.dasifind.backend.global.error.BusinessException;
import com.dasifind.backend.global.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class SearchCardCloseService {

    private final SearchCardRepository searchCardRepository;
    private final UserRepository userRepository;

    public SearchCardCloseService(
            SearchCardRepository searchCardRepository,
            UserRepository userRepository
    ) {
        this.searchCardRepository = searchCardRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public SearchCardCloseResDTO close(
            Long userId,
            Long searchCardId,
            SearchCardCloseReqDTO request
    ) {
        validateUser(userId);
        SearchCard searchCard = searchCardRepository.findByIdForUpdate(searchCardId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        validateOwner(userId, searchCard);
        validateActive(searchCard);
        validateStatusAndReason(request.status(), request.reason());

        searchCard.close(request.status(), request.reason(), LocalDateTime.now());
        return SearchCardCloseResDTO.from(searchCard);
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

    private void validateActive(SearchCard searchCard) {
        if (searchCard.getStatus() != SearchCardStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.INVALID_SEARCH_CARD_STATUS);
        }
    }

    private void validateStatusAndReason(
            SearchCardStatus status,
            SearchCardCloseReason reason
    ) {
        boolean validFound = status == SearchCardStatus.FOUND
                && (reason == SearchCardCloseReason.FOUND_BY_RECOMMENDATION
                || reason == SearchCardCloseReason.FOUND_OTHER_WAY);
        boolean validClosed = status == SearchCardStatus.CLOSED
                && reason == SearchCardCloseReason.SEARCH_STOPPED;
        if (!validFound && !validClosed) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }
}
