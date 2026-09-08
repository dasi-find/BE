package com.dasifind.backend.domain.searchcard.image.dto.response;

import com.dasifind.backend.domain.searchcard.image.entity.SearchCardImage;
import com.dasifind.backend.domain.searchcard.image.model.SearchCardImageType;

public record SearchCardImageUploadResDTO(
        Long imageId,
        String imageUrl,
        SearchCardImageType imageType
) {

    public static SearchCardImageUploadResDTO of(SearchCardImage image, String imageUrl) {
        return new SearchCardImageUploadResDTO(image.getId(), imageUrl, image.getImageType());
    }
}
