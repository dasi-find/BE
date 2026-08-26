package com.dasifind.backend.domain.auth.model;

import com.dasifind.backend.domain.auth.dto.response.LoginResponse;

public record LoginResult(
        LoginResponse response,
        String refreshToken
) {
}
