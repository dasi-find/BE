package com.dasifind.backend.domain.auth.dto.response;

public record LoginResponse(
        AuthUserResponse user,
        String accessToken,
        long accessTokenExpiresInSeconds
) {
}
