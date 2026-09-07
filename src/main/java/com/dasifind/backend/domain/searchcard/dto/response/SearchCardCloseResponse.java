package com.dasifind.backend.domain.searchcard.dto.response;

import com.dasifind.backend.domain.searchcard.entity.SearchCard;
import com.dasifind.backend.domain.searchcard.model.SearchCardStatus;

import java.time.LocalDateTime;

public record SearchCardCloseResponse(
        Long searchCardId,
        SearchCardStatus status,
        LocalDateTime closedAt
) {

    public static SearchCardCloseResponse from(SearchCard searchCard) {
        return new SearchCardCloseResponse(
                searchCard.getId(),
                searchCard.getStatus(),
                searchCard.getClosedAt()
        );
    }
}
