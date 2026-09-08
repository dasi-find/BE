package com.dasifind.backend.domain.auth.model;

import com.dasifind.backend.domain.auth.dto.response.SignupResDTO;

public record SignupResult(
        SignupResDTO response,
        String refreshToken
) {
}
