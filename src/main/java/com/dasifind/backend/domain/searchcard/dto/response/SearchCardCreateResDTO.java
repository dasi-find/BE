package com.dasifind.backend.domain.searchcard.dto.response;

import com.dasifind.backend.domain.searchcard.entity.SearchCard;
import com.dasifind.backend.domain.searchcard.model.SearchCardStatus;

import java.time.LocalDateTime;

public record SearchCardCreateResDTO(
        Long searchCardId,
        SearchCardStatus status,
        LocalDateTime searchExpiresAt,
        int initialCandidateCount
) {

    public static SearchCardCreateResDTO from(SearchCard searchCard) {
        return new SearchCardCreateResDTO(
                searchCard.getId(),
                searchCard.getStatus(),
                searchCard.getSearchExpiresAt(),
                0
        );
    }
}
