package com.dasifind.backend.domain.searchcard.dto.response;

import com.dasifind.backend.domain.searchcard.entity.SearchCard;
import com.dasifind.backend.domain.searchcard.model.SearchCardStatus;

import java.time.LocalDateTime;

public record SearchCardCloseResDTO(
        Long searchCardId,
        SearchCardStatus status,
        LocalDateTime closedAt
) {

    public static SearchCardCloseResDTO from(SearchCard searchCard) {
        return new SearchCardCloseResDTO(
                searchCard.getId(),
                searchCard.getStatus(),
                searchCard.getClosedAt()
        );
    }
}
