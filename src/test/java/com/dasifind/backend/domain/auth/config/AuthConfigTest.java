package com.dasifind.backend.domain.auth.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class AuthConfigTest {

    @Test
    void HmacSHA256_JCA_키로_HS256_JWT를_서명한다() {
        AuthTokenProperties properties = new AuthTokenProperties(
                "dasifind",
                "test-secret-key-that-is-longer-than-32-bytes",
                Duration.ofMinutes(30),
                Duration.ofDays(14),
                "refreshToken",
                false
        );
        JwtEncoder encoder = new AuthConfig().jwtEncoder(properties);
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("dasifind")
                .subject("7")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(1800))
                .build();

        String token = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(),
                claims
        )).getTokenValue();

        assertThat(token.split("\\.")).hasSize(3);
    }
}
