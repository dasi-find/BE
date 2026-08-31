package com.dasifind.backend.domain.auth.cookie;

import com.dasifind.backend.domain.auth.config.AuthTokenProperties;
import com.dasifind.backend.global.error.BusinessException;
import com.dasifind.backend.global.error.ErrorCode;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class RefreshTokenCookieResolver {

    private final AuthTokenProperties tokenProperties;

    public RefreshTokenCookieResolver(AuthTokenProperties tokenProperties) {
        this.tokenProperties = tokenProperties;
    }

    public String resolveRequired(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return Arrays.stream(cookies)
                .filter(cookie -> tokenProperties.refreshCookieName().equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }
}
