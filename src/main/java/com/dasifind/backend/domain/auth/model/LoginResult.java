package com.dasifind.backend.domain.auth.model;

import com.dasifind.backend.domain.auth.dto.response.LoginResDTO;

public record LoginResult(
        LoginResDTO response,
        String refreshToken
) {
}
