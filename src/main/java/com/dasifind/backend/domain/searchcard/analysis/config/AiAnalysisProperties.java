package com.dasifind.backend.domain.searchcard.analysis.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app.ai.analysis")
public record AiAnalysisProperties(
        String baseUrl,
        @NotBlank String path,
        @NotNull Duration connectTimeout,
        @NotNull Duration readTimeout
) {
    public boolean isConfigured() {
        return baseUrl != null && !baseUrl.isBlank();
    }

    public String endpoint() {
        String normalizedBaseUrl = baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;
        return normalizedBaseUrl + path;
    }
}
