package com.dasifind.backend.domain.auth.cookie;

import com.dasifind.backend.domain.auth.config.AuthTokenProperties;
import com.dasifind.backend.domain.auth.config.RefreshTokenCookieSameSite;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class RefreshTokenCookieFactoryTest {

    @Test
    void 운영_환경에서_SameSite_None과_Secure를_함께_설정할_수_있다() {
        AuthTokenProperties properties = properties(true, RefreshTokenCookieSameSite.NONE);

        ResponseCookie cookie = new RefreshTokenCookieFactory(properties).create("refresh-token");

        assertThat(cookie.toString())
                .contains("__Host-refresh_token=refresh-token")
                .contains("Secure")
                .contains("HttpOnly")
                .contains("SameSite=None")
                .contains("Path=/");
    }

    @Test
    void SameSite_None은_Secure_없이_사용할_수_없다() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> properties(false, RefreshTokenCookieSameSite.NONE))
                .withMessageContaining("SameSite=None");
    }

    @Test
    void Host_접두사_쿠키는_Secure_없이_사용할_수_없다() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> properties(false, RefreshTokenCookieSameSite.LAX))
                .withMessageContaining("__Host-");
    }

    private AuthTokenProperties properties(boolean secure, RefreshTokenCookieSameSite sameSite) {
        return new AuthTokenProperties(
                "dasifind",
                "test-secret-key-that-is-longer-than-32-bytes",
                Duration.ofMinutes(30),
                Duration.ofDays(14),
                "__Host-refresh_token",
                secure,
                sameSite
        );
    }
}
