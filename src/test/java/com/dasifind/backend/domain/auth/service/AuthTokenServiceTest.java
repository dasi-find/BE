package com.dasifind.backend.domain.auth.service;

import com.dasifind.backend.domain.auth.config.AuthTokenProperties;
import com.dasifind.backend.domain.auth.config.RefreshTokenCookieSameSite;
import com.dasifind.backend.domain.auth.model.IssuedTokens;
import com.dasifind.backend.domain.auth.repository.RefreshTokenRepository;
import com.dasifind.backend.global.error.BusinessException;
import com.dasifind.backend.global.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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
                true,
                RefreshTokenCookieSameSite.LAX
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

    @Test
    void 리프레시_토큰을_원문이_아닌_해시로_폐기한다() {
        AuthTokenProperties properties = new AuthTokenProperties(
                "dasifind",
                "test-secret-key-that-is-longer-than-32-bytes",
                Duration.ofMinutes(30),
                Duration.ofDays(14),
                "__Host-refresh_token",
                true,
                RefreshTokenCookieSameSite.LAX
        );
        AuthTokenService authTokenService = new AuthTokenService(
                jwtEncoder,
                refreshTokenRepository,
                properties,
                new SecureRandom()
        );

        authTokenService.revokeRefreshToken("refresh-token");

        ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
        verify(refreshTokenRepository).delete(hashCaptor.capture());
        assertThat(hashCaptor.getValue())
                .hasSize(64)
                .matches("[0-9a-f]{64}")
                .isNotEqualTo("refresh-token");
    }

    @Test
    void 유효한_리프레시_토큰을_새_토큰으로_교체한다() {
        AuthTokenService authTokenService = authTokenService();
        Jwt jwt = Jwt.withTokenValue("new-access-token")
                .header("alg", "HS256")
                .claim("sub", "7")
                .build();
        when(refreshTokenRepository.findUserId(anyString())).thenReturn(Optional.of(7L));
        when(jwtEncoder.encode(any())).thenReturn(jwt);
        when(refreshTokenRepository.rotate(anyString(), anyString(), eq(Duration.ofDays(14))))
                .thenReturn(true);

        IssuedTokens tokens = authTokenService.refresh("current-refresh-token");

        assertThat(tokens.accessToken()).isEqualTo("new-access-token");
        assertThat(tokens.accessTokenExpiresInSeconds()).isEqualTo(1800);
        assertThat(tokens.refreshToken()).isNotBlank().isNotEqualTo("current-refresh-token");

        ArgumentCaptor<String> lookupHashCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> currentHashCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> newHashCaptor = ArgumentCaptor.forClass(String.class);
        verify(refreshTokenRepository).findUserId(lookupHashCaptor.capture());
        verify(refreshTokenRepository).rotate(
                currentHashCaptor.capture(),
                newHashCaptor.capture(),
                eq(Duration.ofDays(14))
        );
        assertThat(currentHashCaptor.getValue()).isEqualTo(lookupHashCaptor.getValue());
        assertThat(newHashCaptor.getValue())
                .hasSize(64)
                .matches("[0-9a-f]{64}")
                .isNotEqualTo(currentHashCaptor.getValue())
                .isNotEqualTo(tokens.refreshToken());
    }

    @Test
    void 만료되거나_폐기된_리프레시_토큰은_거절한다() {
        AuthTokenService authTokenService = authTokenService();
        when(refreshTokenRepository.findUserId(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authTokenService.refresh("invalid-refresh-token"))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_TOKEN);

        verify(jwtEncoder, never()).encode(any());
        verify(refreshTokenRepository, never()).rotate(anyString(), anyString(), any());
    }

    @Test
    void 동일한_리프레시_토큰이_먼저_교체되면_재사용을_거절한다() {
        AuthTokenService authTokenService = authTokenService();
        Jwt jwt = Jwt.withTokenValue("unused-access-token")
                .header("alg", "HS256")
                .claim("sub", "7")
                .build();
        when(refreshTokenRepository.findUserId(anyString())).thenReturn(Optional.of(7L));
        when(jwtEncoder.encode(any())).thenReturn(jwt);
        when(refreshTokenRepository.rotate(anyString(), anyString(), eq(Duration.ofDays(14))))
                .thenReturn(false);

        assertThatThrownBy(() -> authTokenService.refresh("already-rotated-token"))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_TOKEN);
    }

    @Test
    void 액세스_토큰_발급이_실패하면_기존_리프레시_토큰을_교체하지_않는다() {
        AuthTokenService authTokenService = authTokenService();
        when(refreshTokenRepository.findUserId(anyString())).thenReturn(Optional.of(7L));
        when(jwtEncoder.encode(any())).thenThrow(new IllegalStateException("JWT encoder unavailable"));

        assertThatThrownBy(() -> authTokenService.refresh("current-refresh-token"))
                .isInstanceOf(IllegalStateException.class);

        verify(refreshTokenRepository, never()).rotate(anyString(), anyString(), any());
    }

    @Test
    void 사용자의_현재_리프레시_토큰을_폐기한다() {
        AuthTokenService authTokenService = authTokenService();
        when(refreshTokenRepository.revoke(anyString(), eq(7L))).thenReturn(true);

        authTokenService.revokeRefreshToken(7L, "refresh-token");

        ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
        verify(refreshTokenRepository).revoke(hashCaptor.capture(), eq(7L));
        assertThat(hashCaptor.getValue())
                .hasSize(64)
                .matches("[0-9a-f]{64}")
                .isNotEqualTo("refresh-token");
    }

    @Test
    void 리프레시_토큰이_없거나_소유자가_다르면_폐기를_거절한다() {
        AuthTokenService authTokenService = authTokenService();
        when(refreshTokenRepository.revoke(anyString(), eq(7L))).thenReturn(false);

        assertThatThrownBy(() -> authTokenService.revokeRefreshToken(7L, "invalid-refresh-token"))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_TOKEN);
    }

    private AuthTokenService authTokenService() {
        AuthTokenProperties properties = new AuthTokenProperties(
                "dasifind",
                "test-secret-key-that-is-longer-than-32-bytes",
                Duration.ofMinutes(30),
                Duration.ofDays(14),
                "__Host-refresh_token",
                true,
                RefreshTokenCookieSameSite.LAX
        );
        return new AuthTokenService(jwtEncoder, refreshTokenRepository, properties, new SecureRandom());
    }
}
