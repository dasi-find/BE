package com.dasifind.backend.domain.searchcard.service;

import com.dasifind.backend.domain.searchcard.dto.response.SearchCardListItemResDTO;
import com.dasifind.backend.domain.searchcard.dto.response.SearchCardListResDTO;
import com.dasifind.backend.domain.searchcard.entity.LostLocation;
import com.dasifind.backend.domain.searchcard.entity.SearchCard;
import com.dasifind.backend.domain.searchcard.model.SearchCardStatus;
import com.dasifind.backend.domain.searchcard.repository.LostLocationRepository;
import com.dasifind.backend.domain.searchcard.repository.SearchCardRepository;
import com.dasifind.backend.domain.user.repository.UserRepository;
import com.dasifind.backend.global.error.BusinessException;
import com.dasifind.backend.global.error.ErrorCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class SearchCardQueryService {

    static final int MAX_PAGE_SIZE = 100;

    private static final Sort LATEST_FIRST = Sort.by(
            Sort.Order.desc("createdAt"),
            Sort.Order.desc("id")
    );

    private final SearchCardRepository searchCardRepository;
    private final LostLocationRepository lostLocationRepository;
    private final UserRepository userRepository;

    public SearchCardQueryService(
            SearchCardRepository searchCardRepository,
            LostLocationRepository lostLocationRepository,
            UserRepository userRepository
    ) {
        this.searchCardRepository = searchCardRepository;
        this.lostLocationRepository = lostLocationRepository;
        this.userRepository = userRepository;
    }

    public SearchCardListResDTO getMySearchCards(
            Long userId,
            SearchCardStatus status,
            int page,
            int size
    ) {
        validateUser(userId);
        validatePage(page, size);

        Pageable pageable = PageRequest.of(page, size, LATEST_FIRST);
        Page<SearchCard> searchCards = status == null
                ? searchCardRepository.findByUserId(userId, pageable)
                : searchCardRepository.findByUserIdAndStatus(userId, status, pageable);

        Map<Long, LostLocation> locationsBySearchCardId = findLocations(searchCards.getContent());
        List<SearchCardListItemResDTO> content = searchCards.getContent().stream()
                .map(searchCard -> SearchCardListItemResDTO.from(
                        searchCard,
                        locationsBySearchCardId.get(searchCard.getId())
                ))
                .toList();

        return SearchCardListResDTO.from(searchCards, content);
    }

    private void validateUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }

    private Map<Long, LostLocation> findLocations(List<SearchCard> searchCards) {
        if (searchCards.isEmpty()) {
            return Map.of();
        }
        List<Long> searchCardIds = searchCards.stream()
                .map(SearchCard::getId)
                .toList();

        return lostLocationRepository.findAllBySearchCardIdIn(searchCardIds).stream()
                .collect(Collectors.toUnmodifiableMap(
                        LostLocation::getSearchCardId,
                        Function.identity()
                ));
    }
}
