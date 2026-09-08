package com.dasifind.backend.domain.searchcard.dto.response;

import com.dasifind.backend.domain.searchcard.analysis.entity.SearchCardAnalysis;

import java.util.List;

public record SearchCardDetailAnalysisResDTO(
        List<String> features,
        String modelVersion
) {

    public SearchCardDetailAnalysisResDTO {
        features = List.copyOf(features);
    }

    public static SearchCardDetailAnalysisResDTO from(SearchCardAnalysis analysis) {
        return new SearchCardDetailAnalysisResDTO(
                analysis.getFeatures(),
                analysis.getModelVersion()
        );
    }
}
