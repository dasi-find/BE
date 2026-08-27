package com.dasifind.backend.domain.auth.controller;

import com.dasifind.backend.domain.auth.cookie.RefreshTokenCookieFactory;
import com.dasifind.backend.domain.auth.dto.request.LoginRequest;
import com.dasifind.backend.domain.auth.dto.response.LoginResponse;
import com.dasifind.backend.domain.auth.model.LoginResult;
import com.dasifind.backend.domain.auth.service.LoginService;
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
public class LoginController {

    private final LoginService loginService;
    private final RefreshTokenCookieFactory refreshTokenCookieFactory;

    public LoginController(LoginService loginService, RefreshTokenCookieFactory refreshTokenCookieFactory) {
        this.loginService = loginService;
        this.refreshTokenCookieFactory = refreshTokenCookieFactory;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request
    ) {
        LoginResult result = loginService.login(request);
        ResponseCookie refreshTokenCookie = refreshTokenCookieFactory.create(result.refreshToken());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                .body(ApiResponse.success(result.response()));
    }
}
