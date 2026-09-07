package com.dasifind.backend.domain.searchcard.dto.response;

import com.dasifind.backend.domain.searchcard.image.entity.SearchCardImage;
import com.dasifind.backend.domain.searchcard.image.model.SearchCardImageType;

public record SearchCardDetailImageResponse(
        Long id,
        String imageUrl,
        SearchCardImageType imageType
) {

    public static SearchCardDetailImageResponse of(SearchCardImage image, String imageUrl) {
        return new SearchCardDetailImageResponse(
                image.getId(),
                imageUrl,
                image.getImageType()
        );
    }
}
