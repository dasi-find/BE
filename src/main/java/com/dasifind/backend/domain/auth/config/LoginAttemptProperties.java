package com.dasifind.backend.domain.auth.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app.auth.login-attempt")
public record LoginAttemptProperties(
        @Min(1) int maxAttempts,
        @NotNull Duration blockDuration
) {
}
