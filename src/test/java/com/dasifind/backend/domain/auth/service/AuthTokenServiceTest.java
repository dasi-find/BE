package com.dasifind.backend.domain.auth.service;

import com.dasifind.backend.domain.auth.config.AuthTokenProperties;
import com.dasifind.backend.domain.auth.model.IssuedTokens;
import com.dasifind.backend.domain.auth.repository.RefreshTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;

import java.security.SecureRandom;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthTokenServiceTest {

    @Mock
    private JwtEncoder jwtEncoder;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Test
    void 액세스_토큰과_리프레시_토큰을_발급하고_리프레시_토큰은_해시로_저장한다() {
        AuthTokenProperties properties = new AuthTokenProperties(
                "dasifind",
                "test-secret-key-that-is-longer-than-32-bytes",
                Duration.ofMinutes(30),
                Duration.ofDays(14),
                "__Host-refresh_token",
                true
        );
        Jwt jwt = Jwt.withTokenValue("access-token")
                .header("alg", "HS256")
                .claim("sub", "7")
                .build();
        when(jwtEncoder.encode(any())).thenReturn(jwt);
        AuthTokenService authTokenService = new AuthTokenService(
                jwtEncoder,
                refreshTokenRepository,
                properties,
                new SecureRandom()
        );

        IssuedTokens tokens = authTokenService.issue(7L);

        assertThat(tokens.accessToken()).isEqualTo("access-token");
        assertThat(tokens.accessTokenExpiresInSeconds()).isEqualTo(1800);
        assertThat(tokens.refreshToken()).isNotBlank();
        ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
        verify(refreshTokenRepository).save(hashCaptor.capture(), org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.eq(Duration.ofDays(14)));
        assertThat(hashCaptor.getValue())
                .hasSize(64)
                .matches("[0-9a-f]{64}")
                .isNotEqualTo(tokens.refreshToken());
    }
}
