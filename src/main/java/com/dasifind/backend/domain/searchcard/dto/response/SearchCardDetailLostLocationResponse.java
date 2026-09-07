package com.dasifind.backend.domain.searchcard.dto.response;

import com.dasifind.backend.domain.searchcard.entity.LostLocation;

import java.math.BigDecimal;

public record SearchCardDetailLostLocationResponse(
        String placeName,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        String description
) {

    public static SearchCardDetailLostLocationResponse from(LostLocation location) {
        return new SearchCardDetailLostLocationResponse(
                location.getPlaceName(),
                location.getAddress(),
                location.getLatitude(),
                location.getLongitude(),
                location.getDescription()
        );
    }
}
