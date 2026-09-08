package com.dasifind.backend.domain.auth.controller;

import com.dasifind.backend.domain.auth.cookie.RefreshTokenCookieFactory;
import com.dasifind.backend.domain.auth.dto.request.SignupReqDTO;
import com.dasifind.backend.domain.auth.dto.response.SignupResDTO;
import com.dasifind.backend.domain.auth.model.SignupResult;
import com.dasifind.backend.domain.auth.service.SignupService;
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
public class SignupController {

    private final SignupService signupService;
    private final RefreshTokenCookieFactory refreshTokenCookieFactory;

    public SignupController(SignupService signupService, RefreshTokenCookieFactory refreshTokenCookieFactory) {
        this.signupService = signupService;
        this.refreshTokenCookieFactory = refreshTokenCookieFactory;
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResDTO<SignupResDTO>> signup(
            @Valid @RequestBody SignupReqDTO request
    ) {
        SignupResult result = signupService.signup(request);
        ResponseCookie refreshTokenCookie = refreshTokenCookieFactory.create(result.refreshToken());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                .body(ApiResDTO.success(result.response()));
    }
}
