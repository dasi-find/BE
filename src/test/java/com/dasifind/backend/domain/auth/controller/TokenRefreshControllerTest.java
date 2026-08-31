package com.dasifind.backend.domain.auth.controller;

import com.dasifind.backend.domain.auth.model.IssuedTokens;
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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TokenRefreshControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthTokenService authTokenService;

    @Test
    void 유효한_리프레시_토큰으로_액세스_토큰과_새_쿠키를_발급한다() throws Exception {
        when(authTokenService.refresh("current-refresh-token"))
                .thenReturn(new IssuedTokens("new-access-token", 1800, "new-refresh-token"));

        mockMvc.perform(post("/api/v1/auth/token/refresh")
                        .cookie(new Cookie("__Host-refresh_token", "current-refresh-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("COMMON2001"))
                .andExpect(jsonPath("$.result.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.result.accessTokenExpiresInSeconds").value(1800))
                .andExpect(jsonPath("$.result.refreshToken").doesNotExist())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("__Host-refresh_token=new-refresh-token"),
                        org.hamcrest.Matchers.containsString("Path=/"),
                        org.hamcrest.Matchers.containsString("Secure"),
                        org.hamcrest.Matchers.containsString("HttpOnly"),
                        org.hamcrest.Matchers.containsString("SameSite=Lax")
                )));

        verify(authTokenService).refresh("current-refresh-token");
    }

    @Test
    void 리프레시_토큰_쿠키가_없으면_로그인_필요로_응답한다() throws Exception {
        mockMvc.perform(post("/api/v1/auth/token/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("COMMON4011"));
    }

    @Test
    void 리프레시_토큰_쿠키가_비어_있으면_로그인_필요로_응답한다() throws Exception {
        mockMvc.perform(post("/api/v1/auth/token/refresh")
                        .cookie(new Cookie("__Host-refresh_token", "")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("COMMON4011"));
    }

    @Test
    void 만료되거나_폐기된_리프레시_토큰은_유효하지_않은_토큰으로_응답한다() throws Exception {
        when(authTokenService.refresh("invalid-refresh-token"))
                .thenThrow(new BusinessException(ErrorCode.INVALID_TOKEN));

        mockMvc.perform(post("/api/v1/auth/token/refresh")
                        .cookie(new Cookie("__Host-refresh_token", "invalid-refresh-token")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH4012"));
    }
}
