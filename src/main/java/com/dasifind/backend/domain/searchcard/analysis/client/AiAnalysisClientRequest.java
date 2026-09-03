package com.dasifind.backend.domain.searchcard.analysis.client;

import com.dasifind.backend.domain.searchcard.image.model.SearchCardImageType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record AiAnalysisClientRequest(
        String category,
        String itemName,
        List<String> colors,
        String brand,
        String featureDescription,
        List<Image> images,
        LocalDate lostDate,
        LocalTime lostStartTime,
        LocalTime lostEndTime,
        Location lostLocation
) {
    public record Image(
            Long imageId,
            String imageUrl,
            SearchCardImageType imageType
    ) {
    }

    public record Location(
            String placeName,
            String address,
            BigDecimal latitude,
            BigDecimal longitude,
            String description
    ) {
    }
}
