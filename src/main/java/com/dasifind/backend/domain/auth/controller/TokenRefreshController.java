package com.dasifind.backend.domain.auth.controller;

import com.dasifind.backend.domain.auth.config.AuthTokenProperties;
import com.dasifind.backend.domain.auth.cookie.RefreshTokenCookieFactory;
import com.dasifind.backend.domain.auth.dto.response.TokenRefreshResponse;
import com.dasifind.backend.domain.auth.model.IssuedTokens;
import com.dasifind.backend.domain.auth.service.AuthTokenService;
import com.dasifind.backend.global.api.ApiResponse;
import com.dasifind.backend.global.error.BusinessException;
import com.dasifind.backend.global.error.ErrorCode;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

@RestController
@RequestMapping("/api/v1/auth/token")
public class TokenRefreshController {

    private final AuthTokenService authTokenService;
    private final AuthTokenProperties tokenProperties;
    private final RefreshTokenCookieFactory refreshTokenCookieFactory;

    public TokenRefreshController(
            AuthTokenService authTokenService,
            AuthTokenProperties tokenProperties,
            RefreshTokenCookieFactory refreshTokenCookieFactory
    ) {
        this.authTokenService = authTokenService;
        this.tokenProperties = tokenProperties;
        this.refreshTokenCookieFactory = refreshTokenCookieFactory;
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenRefreshResponse>> refresh(HttpServletRequest request) {
        String refreshToken = findRefreshToken(request);
        IssuedTokens tokens = authTokenService.refresh(refreshToken);
        ResponseCookie refreshTokenCookie = refreshTokenCookieFactory.create(tokens.refreshToken());
        TokenRefreshResponse response = new TokenRefreshResponse(
                tokens.accessToken(),
                tokens.accessTokenExpiresInSeconds()
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                .body(ApiResponse.success(response));
    }

    private String findRefreshToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return Arrays.stream(cookies)
                .filter(cookie -> tokenProperties.refreshCookieName().equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> !value.isBlank())
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }
}
