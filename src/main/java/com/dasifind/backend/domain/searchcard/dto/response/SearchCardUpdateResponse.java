package com.dasifind.backend.domain.searchcard.dto.response;

import com.dasifind.backend.domain.searchcard.entity.SearchCard;
import com.dasifind.backend.domain.searchcard.model.SearchCardStatus;

public record SearchCardUpdateResponse(
        Long searchCardId,
        SearchCardStatus status,
        boolean rematchScheduled
) {

    public static SearchCardUpdateResponse from(SearchCard searchCard) {
        return new SearchCardUpdateResponse(
                searchCard.getId(),
                searchCard.getStatus(),
                false
        );
    }
}
