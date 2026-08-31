package com.dasifind.backend.domain.searchcard.image.dto.response;

import com.dasifind.backend.domain.searchcard.image.entity.SearchCardImage;
import com.dasifind.backend.domain.searchcard.image.model.SearchCardImageType;

public record SearchCardImageUploadResponse(
        Long imageId,
        String imageUrl,
        SearchCardImageType imageType
) {

    public static SearchCardImageUploadResponse of(SearchCardImage image, String imageUrl) {
        return new SearchCardImageUploadResponse(image.getId(), imageUrl, image.getImageType());
    }
}
