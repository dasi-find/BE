package com.dasifind.backend.domain.auth.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.JwsHeader;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthConfigTest {

    @Test
    void HmacSHA256_JCA_키로_HS256_JWT를_서명한다() {
        AuthTokenProperties properties = properties();
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

    @Test
    void 서명_만료_발급자_액세스_토큰_유형과_사용자_아이디를_검증한다() {
        AuthTokenProperties properties = properties();
        JwtDecoder decoder = new AuthConfig().jwtDecoder(properties);

        var jwt = decoder.decode(encode(properties, "dasifind", "7", "access"));

        assertThat(jwt.getSubject()).isEqualTo("7");
        assertThat(jwt.getClaimAsString("tokenType")).isEqualTo("access");
    }

    @Test
    void 발급자가_다른_토큰을_거절한다() {
        AuthTokenProperties properties = properties();
        JwtDecoder decoder = new AuthConfig().jwtDecoder(properties);

        assertThatThrownBy(() -> decoder.decode(encode(properties, "other", "7", "access")))
                .isInstanceOf(JwtValidationException.class);
    }

    @Test
    void 액세스_토큰이_아닌_토큰을_거절한다() {
        AuthTokenProperties properties = properties();
        JwtDecoder decoder = new AuthConfig().jwtDecoder(properties);

        assertThatThrownBy(() -> decoder.decode(encode(properties, "dasifind", "7", "refresh")))
                .isInstanceOf(JwtValidationException.class);
    }

    @Test
    void 사용자_아이디가_양의_정수가_아니면_거절한다() {
        AuthTokenProperties properties = properties();
        JwtDecoder decoder = new AuthConfig().jwtDecoder(properties);

        assertThatThrownBy(() -> decoder.decode(encode(properties, "dasifind", "invalid", "access")))
                .isInstanceOf(JwtValidationException.class);
    }

    private String encode(
            AuthTokenProperties properties,
            String issuer,
            String subject,
            String tokenType
    ) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(subject)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(1800))
                .claim("tokenType", tokenType)
                .build();
        return new AuthConfig().jwtEncoder(properties).encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(),
                claims
        )).getTokenValue();
    }

    private AuthTokenProperties properties() {
        return new AuthTokenProperties(
                "dasifind",
                "test-secret-key-that-is-longer-than-32-bytes",
                Duration.ofMinutes(30),
                Duration.ofDays(14),
                "refreshToken",
                false,
                RefreshTokenCookieSameSite.LAX
        );
    }
}
