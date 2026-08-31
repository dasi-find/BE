package com.dasifind.backend.domain.auth.service;

import com.dasifind.backend.domain.auth.config.AuthTokenProperties;
import com.dasifind.backend.domain.auth.model.IssuedTokens;
import com.dasifind.backend.domain.auth.repository.RefreshTokenRepository;
import com.dasifind.backend.global.error.BusinessException;
import com.dasifind.backend.global.error.ErrorCode;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class AuthTokenService {

    private static final int REFRESH_TOKEN_BYTES = 32;

    private final JwtEncoder jwtEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthTokenProperties properties;
    private final SecureRandom secureRandom;

    public AuthTokenService(
            JwtEncoder jwtEncoder,
            RefreshTokenRepository refreshTokenRepository,
            AuthTokenProperties properties,
            SecureRandom secureRandom
    ) {
        this.jwtEncoder = jwtEncoder;
        this.refreshTokenRepository = refreshTokenRepository;
        this.properties = properties;
        this.secureRandom = secureRandom;
    }

    public IssuedTokens issue(Long userId) {
        String accessToken = issueAccessToken(userId);
        String refreshToken = randomToken();
        refreshTokenRepository.save(hash(refreshToken), userId, properties.refreshTtl());

        return issuedTokens(accessToken, refreshToken);
    }

    public IssuedTokens refresh(String currentRefreshToken) {
        String currentTokenHash = hash(currentRefreshToken);
        Long userId = refreshTokenRepository.findUserId(currentTokenHash)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN));

        String accessToken = issueAccessToken(userId);
        String newRefreshToken = randomToken();
        boolean rotated = refreshTokenRepository.rotate(
                currentTokenHash,
                hash(newRefreshToken),
                properties.refreshTtl()
        );
        if (!rotated) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        return issuedTokens(accessToken, newRefreshToken);
    }

    public void revokeRefreshToken(String refreshToken) {
        refreshTokenRepository.delete(hash(refreshToken));
    }

    public void revokeRefreshToken(Long userId, String refreshToken) {
        boolean revoked = refreshTokenRepository.revoke(hash(refreshToken), userId);
        if (!revoked) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
    }

    private String issueAccessToken(Long userId) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(properties.accessTtl());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .subject(userId.toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("tokenType", "access")
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder
                .encode(JwtEncoderParameters.from(header, claims))
                .getTokenValue();
    }

    private IssuedTokens issuedTokens(String accessToken, String refreshToken) {
        return new IssuedTokens(
                accessToken,
                properties.accessTtl().toSeconds(),
                refreshToken
        );
    }

    private String randomToken() {
        byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
