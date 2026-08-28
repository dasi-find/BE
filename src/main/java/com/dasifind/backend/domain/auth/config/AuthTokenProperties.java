package com.dasifind.backend.domain.auth.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app.auth.token")
public record AuthTokenProperties(
        @NotBlank String issuer,
        @NotBlank @Size(min = 32) String secret,
        @NotNull Duration accessTtl,
        @NotNull Duration refreshTtl,
        @NotBlank String refreshCookieName,
        boolean secureCookie,
        @NotNull RefreshTokenCookieSameSite cookieSameSite
) {
    public AuthTokenProperties {
        if (cookieSameSite == RefreshTokenCookieSameSite.NONE && !secureCookie) {
            throw new IllegalArgumentException("SameSite=None refresh token cookies must be Secure");
        }
        if (refreshCookieName != null && refreshCookieName.startsWith("__Host-") && !secureCookie) {
            throw new IllegalArgumentException("__Host- refresh token cookies must be Secure");
        }
    }
}
