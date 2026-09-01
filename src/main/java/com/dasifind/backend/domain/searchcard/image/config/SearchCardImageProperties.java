package com.dasifind.backend.domain.searchcard.image.config;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app.search-card-image")
public record SearchCardImageProperties(
        @NotNull DataSize maxFileSize,
        @NotNull Duration orphanRetention
) {
}
