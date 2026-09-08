package com.dasifind.backend.domain.searchcard.dto.response;

import com.dasifind.backend.domain.searchcard.entity.LostLocation;
import com.dasifind.backend.domain.searchcard.entity.SearchCard;
import com.dasifind.backend.domain.searchcard.model.SearchCardStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record SearchCardListItemResDTO(
        Long id,
        String itemName,
        SearchCardStatus status,
        LocalDate lostDate,
        String lostPlaceName,
        BigDecimal bestCandidateScore,
        LocalDateTime searchExpiresAt
) {

    public static SearchCardListItemResDTO from(
            SearchCard searchCard,
            LostLocation lostLocation
    ) {
        return new SearchCardListItemResDTO(
                searchCard.getId(),
                searchCard.getItemName(),
                searchCard.getStatus(),
                searchCard.getLostDate(),
                lostLocation.getPlaceName(),
                null,
                searchCard.getSearchExpiresAt()
        );
    }
}
