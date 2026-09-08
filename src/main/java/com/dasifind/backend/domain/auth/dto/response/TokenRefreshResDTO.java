package com.dasifind.backend.domain.auth.dto.response;

public record TokenRefreshResDTO(
        String accessToken,
        long accessTokenExpiresInSeconds
) {
}
