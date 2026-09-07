package com.dasifind.backend.domain.searchcard.dto.response;

import com.dasifind.backend.domain.searchcard.analysis.entity.SearchCardAnalysis;
import com.dasifind.backend.domain.searchcard.entity.LostLocation;
import com.dasifind.backend.domain.searchcard.entity.SearchCard;
import com.dasifind.backend.domain.searchcard.model.SearchCardStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public record SearchCardDetailResponse(
        Long id,
        String category,
        String itemName,
        List<String> colors,
        String brand,
        String material,
        String featureDescription,
        List<SearchCardDetailImageResponse> images,
        LocalDate lostDate,
        LocalTime lostStartTime,
        LocalTime lostEndTime,
        SearchCardDetailLostLocationResponse lostLocation,
        SearchCardDetailAnalysisResponse analysis,
        SearchCardStatus status,
        LocalDateTime searchExpiresAt,
        int candidateCount,
        BigDecimal bestCandidateScore
) {

    public SearchCardDetailResponse {
        colors = List.copyOf(colors);
        images = List.copyOf(images);
    }

    public static SearchCardDetailResponse of(
            SearchCard searchCard,
            LostLocation lostLocation,
            SearchCardAnalysis analysis,
            List<SearchCardDetailImageResponse> images
    ) {
        return new SearchCardDetailResponse(
                searchCard.getId(),
                searchCard.getCategory(),
                searchCard.getItemName(),
                searchCard.getColors(),
                searchCard.getBrand(),
                searchCard.getMaterial(),
                searchCard.getFeatureDescription(),
                images,
                searchCard.getLostDate(),
                searchCard.getLostStartTime(),
                searchCard.getLostEndTime(),
                SearchCardDetailLostLocationResponse.from(lostLocation),
                SearchCardDetailAnalysisResponse.from(analysis),
                searchCard.getStatus(),
                searchCard.getSearchExpiresAt(),
                0,
                null
        );
    }
}
