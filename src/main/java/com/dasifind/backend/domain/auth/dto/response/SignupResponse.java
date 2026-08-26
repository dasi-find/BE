package com.dasifind.backend.domain.auth.dto.response;

public record SignupResponse(
        AuthUserResponse user,
        String accessToken,
        long accessTokenExpiresInSeconds
) {
}
