package com.dasifind.backend.domain.auth.controller;

import com.dasifind.backend.domain.auth.cookie.RefreshTokenCookieFactory;
import com.dasifind.backend.domain.auth.cookie.RefreshTokenCookieResolver;
import com.dasifind.backend.domain.auth.dto.response.TokenRefreshResDTO;
import com.dasifind.backend.domain.auth.model.IssuedTokens;
import com.dasifind.backend.domain.auth.service.AuthTokenService;
import com.dasifind.backend.global.api.ApiResDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/token")
public class TokenRefreshController {

    private final AuthTokenService authTokenService;
    private final RefreshTokenCookieResolver refreshTokenCookieResolver;
    private final RefreshTokenCookieFactory refreshTokenCookieFactory;

    public TokenRefreshController(
            AuthTokenService authTokenService,
            RefreshTokenCookieResolver refreshTokenCookieResolver,
            RefreshTokenCookieFactory refreshTokenCookieFactory
    ) {
        this.authTokenService = authTokenService;
        this.refreshTokenCookieResolver = refreshTokenCookieResolver;
        this.refreshTokenCookieFactory = refreshTokenCookieFactory;
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResDTO<TokenRefreshResDTO>> refresh(HttpServletRequest request) {
        String refreshToken = refreshTokenCookieResolver.resolveRequired(request);
        IssuedTokens tokens = authTokenService.refresh(refreshToken);
        ResponseCookie refreshTokenCookie = refreshTokenCookieFactory.create(tokens.refreshToken());
        TokenRefreshResDTO response = new TokenRefreshResDTO(
                tokens.accessToken(),
                tokens.accessTokenExpiresInSeconds()
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                .body(ApiResDTO.success(response));
    }

}
