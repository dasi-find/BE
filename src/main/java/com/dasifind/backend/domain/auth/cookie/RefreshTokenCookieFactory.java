package com.dasifind.backend.domain.auth.cookie;

import com.dasifind.backend.domain.auth.config.AuthTokenProperties;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenCookieFactory {

    private final AuthTokenProperties properties;

    public RefreshTokenCookieFactory(AuthTokenProperties properties) {
        this.properties = properties;
    }

    public ResponseCookie create(String refreshToken) {
        return ResponseCookie
                .from(properties.refreshCookieName(), refreshToken)
                .httpOnly(true)
                .secure(properties.secureCookie())
                .sameSite(properties.cookieSameSite().attributeValue())
                .path("/")
                .maxAge(properties.refreshTtl())
                .build();
    }
}
