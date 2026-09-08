package com.dasifind.backend.domain.searchcard.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

public record SearchCardListResDTO(
        List<SearchCardListItemResDTO> content,
        int page,
        int size,
        long totalElements,
        boolean hasNext
) {

    public static SearchCardListResDTO from(
            Page<?> searchCards,
            List<SearchCardListItemResDTO> content
    ) {
        return new SearchCardListResDTO(
                List.copyOf(content),
                searchCards.getNumber(),
                searchCards.getSize(),
                searchCards.getTotalElements(),
                searchCards.hasNext()
        );
    }
}
