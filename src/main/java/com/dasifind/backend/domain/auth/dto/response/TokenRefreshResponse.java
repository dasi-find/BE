package com.dasifind.backend.domain.auth.dto.response;

public record TokenRefreshResponse(
        String accessToken,
        long accessTokenExpiresInSeconds
) {
}
