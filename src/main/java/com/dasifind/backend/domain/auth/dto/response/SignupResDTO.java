package com.dasifind.backend.domain.auth.dto.response;

public record SignupResDTO(
        AuthUserResDTO user,
        String accessToken,
        long accessTokenExpiresInSeconds
) {
}
