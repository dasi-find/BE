package com.dasifind.backend.domain.searchcard.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

public record SearchCardListResponse(
        List<SearchCardListItemResponse> content,
        int page,
        int size,
        long totalElements,
        boolean hasNext
) {

    public static SearchCardListResponse from(
            Page<?> searchCards,
            List<SearchCardListItemResponse> content
    ) {
        return new SearchCardListResponse(
                List.copyOf(content),
                searchCards.getNumber(),
                searchCards.getSize(),
                searchCards.getTotalElements(),
                searchCards.hasNext()
        );
    }
}
