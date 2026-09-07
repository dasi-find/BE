package com.dasifind.backend.domain.searchcard.dto.response;

import com.dasifind.backend.domain.searchcard.analysis.entity.SearchCardAnalysis;

import java.util.List;

public record SearchCardDetailAnalysisResponse(
        List<String> features,
        String modelVersion
) {

    public SearchCardDetailAnalysisResponse {
        features = List.copyOf(features);
    }

    public static SearchCardDetailAnalysisResponse from(SearchCardAnalysis analysis) {
        return new SearchCardDetailAnalysisResponse(
                analysis.getFeatures(),
                analysis.getModelVersion()
        );
    }
}
