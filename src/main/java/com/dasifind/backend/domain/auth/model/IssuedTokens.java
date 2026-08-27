package com.dasifind.backend.domain.auth.model;

public record IssuedTokens(
        String accessToken,
        long accessTokenExpiresInSeconds,
        String refreshToken
) {
}
