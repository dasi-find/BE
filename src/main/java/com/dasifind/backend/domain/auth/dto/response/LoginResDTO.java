package com.dasifind.backend.domain.auth.dto.response;

public record LoginResDTO(
        AuthUserResDTO user,
        String accessToken,
        long accessTokenExpiresInSeconds
) {
}
