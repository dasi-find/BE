package com.dasifind.backend.domain.auth.controller;

import com.dasifind.backend.domain.auth.cookie.RefreshTokenCookieFactory;
import com.dasifind.backend.domain.auth.cookie.RefreshTokenCookieResolver;
import com.dasifind.backend.domain.auth.service.AuthTokenService;
import com.dasifind.backend.global.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class LogoutController {

    private final AuthTokenService authTokenService;
    private final RefreshTokenCookieResolver refreshTokenCookieResolver;
    private final RefreshTokenCookieFactory refreshTokenCookieFactory;

    public LogoutController(
            AuthTokenService authTokenService,
            RefreshTokenCookieResolver refreshTokenCookieResolver,
            RefreshTokenCookieFactory refreshTokenCookieFactory
    ) {
        this.authTokenService = authTokenService;
        this.refreshTokenCookieResolver = refreshTokenCookieResolver;
        this.refreshTokenCookieFactory = refreshTokenCookieFactory;
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request
    ) {
        String refreshToken = refreshTokenCookieResolver.resolveRequired(request);
        authTokenService.revokeRefreshToken(Long.valueOf(jwt.getSubject()), refreshToken);
        ResponseCookie expiredRefreshTokenCookie = refreshTokenCookieFactory.expire();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, expiredRefreshTokenCookie.toString())
                .body(ApiResponse.success());
    }
}
