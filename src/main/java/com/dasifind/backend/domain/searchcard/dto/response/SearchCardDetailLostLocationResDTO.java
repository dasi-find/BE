package com.dasifind.backend.domain.searchcard.dto.response;

import com.dasifind.backend.domain.searchcard.entity.LostLocation;

import java.math.BigDecimal;

public record SearchCardDetailLostLocationResDTO(
        String placeName,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        String description
) {

    public static SearchCardDetailLostLocationResDTO from(LostLocation location) {
        return new SearchCardDetailLostLocationResDTO(
                location.getPlaceName(),
                location.getAddress(),
                location.getLatitude(),
                location.getLongitude(),
                location.getDescription()
        );
    }
}
