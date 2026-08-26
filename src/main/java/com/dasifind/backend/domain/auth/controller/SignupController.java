package com.dasifind.backend.domain.auth.controller;

import com.dasifind.backend.domain.auth.config.AuthTokenProperties;
import com.dasifind.backend.domain.auth.dto.request.SignupRequest;
import com.dasifind.backend.domain.auth.dto.response.SignupResponse;
import com.dasifind.backend.domain.auth.model.SignupResult;
import com.dasifind.backend.domain.auth.service.SignupService;
import com.dasifind.backend.global.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class SignupController {

    private final SignupService signupService;
    private final AuthTokenProperties tokenProperties;

    public SignupController(SignupService signupService, AuthTokenProperties tokenProperties) {
        this.signupService = signupService;
        this.tokenProperties = tokenProperties;
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(
            @Valid @RequestBody SignupRequest request
    ) {
        SignupResult result = signupService.signup(request);
        ResponseCookie refreshTokenCookie = ResponseCookie
                .from(tokenProperties.refreshCookieName(), result.refreshToken())
                .httpOnly(true)
                .secure(tokenProperties.secureCookie())
                .sameSite("Lax")
                .path("/")
                .maxAge(tokenProperties.refreshTtl())
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                .body(ApiResponse.success(result.response()));
    }
}
