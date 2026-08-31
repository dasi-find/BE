package com.dasifind.backend.domain.auth.security;

import com.dasifind.backend.global.error.ErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ApiSecurityErrorWriter errorWriter;

    public ApiAuthenticationEntryPoint(ApiSecurityErrorWriter errorWriter) {
        this.errorWriter = errorWriter;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {
        ErrorCode errorCode = isInvalidToken(exception)
                ? ErrorCode.INVALID_TOKEN
                : ErrorCode.UNAUTHORIZED;
        errorWriter.write(response, errorCode);
    }

    private boolean isInvalidToken(AuthenticationException exception) {
        return exception instanceof OAuth2AuthenticationException oauthException
                && OAuth2ErrorCodes.INVALID_TOKEN.equals(oauthException.getError().getErrorCode());
    }
}
