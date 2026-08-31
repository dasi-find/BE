package com.dasifind.backend.domain.auth.cookie;

import com.dasifind.backend.domain.auth.config.AuthTokenProperties;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RefreshTokenCookieFactory {

    private final AuthTokenProperties properties;

    public RefreshTokenCookieFactory(AuthTokenProperties properties) {
        this.properties = properties;
    }

    public ResponseCookie create(String refreshToken) {
        return cookie(refreshToken, properties.refreshTtl());
    }

    public ResponseCookie expire() {
        return cookie("", Duration.ZERO);
    }

    private ResponseCookie cookie(String refreshToken, Duration maxAge) {
        return ResponseCookie
                .from(properties.refreshCookieName(), refreshToken)
                .httpOnly(true)
                .secure(properties.secureCookie())
                .sameSite(properties.cookieSameSite().attributeValue())
                .path("/")
                .maxAge(maxAge)
                .build();
    }
}
