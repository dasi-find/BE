package com.dasifind.backend.domain.searchcard.dto.response;

import com.dasifind.backend.domain.searchcard.entity.SearchCard;
import com.dasifind.backend.domain.searchcard.model.SearchCardStatus;

import java.time.LocalDateTime;

public record SearchCardCreateResponse(
        Long searchCardId,
        SearchCardStatus status,
        LocalDateTime searchExpiresAt,
        int initialCandidateCount
) {

    public static SearchCardCreateResponse from(SearchCard searchCard) {
        return new SearchCardCreateResponse(
                searchCard.getId(),
                searchCard.getStatus(),
                searchCard.getSearchExpiresAt(),
                0
        );
    }
}
