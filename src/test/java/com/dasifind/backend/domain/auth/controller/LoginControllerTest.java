package com.dasifind.backend.domain.auth.controller;

import com.dasifind.backend.domain.auth.dto.response.AuthUserResponse;
import com.dasifind.backend.domain.auth.dto.response.LoginResponse;
import com.dasifind.backend.domain.auth.model.LoginResult;
import com.dasifind.backend.domain.auth.service.LoginService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LoginControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LoginService loginService;

    @Test
    void 로그인_API가_사용자와_액세스_토큰을_반환하고_리프레시_토큰은_쿠키로_전달한다() throws Exception {
        LoginResponse response = new LoginResponse(
                new AuthUserResponse(7L, "user@example.com", "민준"),
                "access-token",
                1800
        );
        when(loginService.login(any())).thenReturn(new LoginResult(response, "refresh-token"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result.user.id").value(7))
                .andExpect(jsonPath("$.result.accessToken").value("access-token"))
                .andExpect(jsonPath("$.result.accessTokenExpiresInSeconds").value(1800))
                .andExpect(jsonPath("$.result.refreshToken").doesNotExist())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("__Host-refresh_token=refresh-token"),
                        org.hamcrest.Matchers.containsString("Secure"),
                        org.hamcrest.Matchers.containsString("HttpOnly"),
                        org.hamcrest.Matchers.containsString("SameSite=Lax")
                )));
    }

    @Test
    void 이메일_형식이_잘못되면_공통_오류를_반환한다() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest().replace("user@example.com", "invalid-email")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON4001"));
    }

    private String validRequest() {
        return """
                {
                  "email":"user@example.com",
                  "password":"password123"
                }
                """;
    }
}
