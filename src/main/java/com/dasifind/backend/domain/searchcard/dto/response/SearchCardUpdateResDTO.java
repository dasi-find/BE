package com.dasifind.backend.domain.searchcard.dto.response;

import com.dasifind.backend.domain.searchcard.entity.SearchCard;
import com.dasifind.backend.domain.searchcard.model.SearchCardStatus;

public record SearchCardUpdateResDTO(
        Long searchCardId,
        SearchCardStatus status,
        boolean rematchScheduled
) {

    public static SearchCardUpdateResDTO from(SearchCard searchCard) {
        return new SearchCardUpdateResDTO(
                searchCard.getId(),
                searchCard.getStatus(),
                false
        );
    }
}
