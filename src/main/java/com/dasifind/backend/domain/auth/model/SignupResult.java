package com.dasifind.backend.domain.auth.model;

import com.dasifind.backend.domain.auth.dto.response.SignupResponse;

public record SignupResult(
        SignupResponse response,
        String refreshToken
) {
}
