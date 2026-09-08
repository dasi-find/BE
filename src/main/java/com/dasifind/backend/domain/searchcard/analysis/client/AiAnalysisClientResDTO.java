package com.dasifind.backend.domain.searchcard.analysis.client;

import java.util.List;

public record AiAnalysisClientResDTO(
        String category,
        String itemName,
        List<String> colors,
        String brand,
        List<String> materials,
        String ocrText,
        List<String> features,
        String modelVersion
) {
}
