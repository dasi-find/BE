package com.dasifind.backend.domain.searchcard.analysis.dto.response;

import com.dasifind.backend.domain.searchcard.analysis.entity.SearchCardAnalysis;

import java.util.List;

public record SearchCardAnalysisResponse(
        Long analysisId,
        String category,
        String itemName,
        List<String> colors,
        String brand,
        List<String> materials,
        String ocrText,
        List<String> features,
        String modelVersion
) {
    public static SearchCardAnalysisResponse from(SearchCardAnalysis analysis) {
        return new SearchCardAnalysisResponse(
                analysis.getId(),
                analysis.getCategory(),
                analysis.getItemName(),
                List.copyOf(analysis.getColors()),
                analysis.getBrand(),
                List.copyOf(analysis.getMaterials()),
                analysis.getOcrText(),
                List.copyOf(analysis.getFeatures()),
                analysis.getModelVersion()
        );
    }
}
