package com.dasifind.backend.domain.auth.controller;

import com.dasifind.backend.domain.auth.service.AuthTokenService;
import com.dasifind.backend.global.error.BusinessException;
import com.dasifind.backend.global.error.ErrorCode;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LogoutControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthTokenService authTokenService;

    @Test
    void 현재_세션을_폐기하고_리프레시_토큰_쿠키를_만료시킨다() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .with(jwt().jwt(jwt -> jwt.subject("7").claim("tokenType", "access")))
                        .cookie(new Cookie("__Host-refresh_token", "refresh-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("COMMON2001"))
                .andExpect(jsonPath("$.result").doesNotExist())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("__Host-refresh_token="),
                        org.hamcrest.Matchers.containsString("Max-Age=0"),
                        org.hamcrest.Matchers.containsString("Path=/"),
                        org.hamcrest.Matchers.containsString("Secure"),
                        org.hamcrest.Matchers.containsString("HttpOnly"),
                        org.hamcrest.Matchers.containsString("SameSite=Lax")
                )));

        verify(authTokenService).revokeRefreshToken(7L, "refresh-token");
    }

    @Test
    void Authorization_헤더가_없으면_로그인_필요로_응답한다() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .cookie(new Cookie("__Host-refresh_token", "refresh-token")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("COMMON4011"));
    }

    @Test
    void 유효하지_않은_Access_Token은_거절한다() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
                        .cookie(new Cookie("__Host-refresh_token", "refresh-token")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH4012"));
    }

    @Test
    void 리프레시_토큰_쿠키가_없으면_로그인_필요로_응답한다() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .with(jwt().jwt(jwt -> jwt.subject("7").claim("tokenType", "access"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("COMMON4011"));
    }

    @Test
    void 리프레시_토큰이_없거나_소유자가_다르면_거절한다() throws Exception {
        doThrow(new BusinessException(ErrorCode.INVALID_TOKEN))
                .when(authTokenService)
                .revokeRefreshToken(7L, "invalid-refresh-token");

        mockMvc.perform(post("/api/v1/auth/logout")
                        .with(jwt().jwt(jwt -> jwt.subject("7").claim("tokenType", "access")))
                        .cookie(new Cookie("__Host-refresh_token", "invalid-refresh-token")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH4012"));
    }
}
