package com.dasifind.backend.domain.searchcard.dto.response;

import com.dasifind.backend.domain.searchcard.image.entity.SearchCardImage;
import com.dasifind.backend.domain.searchcard.image.model.SearchCardImageType;

public record SearchCardDetailImageResDTO(
        Long id,
        String imageUrl,
        SearchCardImageType imageType
) {

    public static SearchCardDetailImageResDTO of(SearchCardImage image, String imageUrl) {
        return new SearchCardDetailImageResDTO(
                image.getId(),
                imageUrl,
                image.getImageType()
        );
    }
}
