package com.dasifind.backend.domain.auth.controller;

import com.dasifind.backend.domain.auth.cookie.RefreshTokenCookieFactory;
import com.dasifind.backend.domain.auth.dto.request.LoginReqDTO;
import com.dasifind.backend.domain.auth.dto.response.LoginResDTO;
import com.dasifind.backend.domain.auth.model.LoginResult;
import com.dasifind.backend.domain.auth.service.LoginService;
import com.dasifind.backend.global.api.ApiResDTO;
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
    public ResponseEntity<ApiResDTO<LoginResDTO>> login(
            @Valid @RequestBody LoginReqDTO request
    ) {
        LoginResult result = loginService.login(request);
        ResponseCookie refreshTokenCookie = refreshTokenCookieFactory.create(result.refreshToken());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                .body(ApiResDTO.success(result.response()));
    }
}
