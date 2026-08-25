package com.dasifind.backend.domain.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.auth.email-verification")
public record EmailVerificationProperties(
        Duration codeTtl,
        Duration resendCooldown,
        int maxAttempts,
        Duration tokenTtl
) {
}
