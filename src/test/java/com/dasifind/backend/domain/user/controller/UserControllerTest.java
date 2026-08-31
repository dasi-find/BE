package com.dasifind.backend.domain.user.controller;

import com.dasifind.backend.domain.user.dto.response.MyProfileResponse;
import com.dasifind.backend.domain.user.service.UserQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserQueryService userQueryService;

    @Test
    void 로그인한_사용자의_내_정보를_조회한다() throws Exception {
        when(userQueryService.getMyProfile(7L)).thenReturn(new MyProfileResponse(
                7L,
                "hello@dasifind.kr",
                "민준",
                true,
                LocalDateTime.of(2026, 8, 25, 10, 0)
        ));

        mockMvc.perform(get("/api/v1/users/me")
                        .with(jwt().jwt(jwt -> jwt.subject("7").claim("tokenType", "access"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("COMMON2001"))
                .andExpect(jsonPath("$.result.id").value(7))
                .andExpect(jsonPath("$.result.email").value("hello@dasifind.kr"))
                .andExpect(jsonPath("$.result.name").value("민준"))
                .andExpect(jsonPath("$.result.emailNotificationEnabled").value(true))
                .andExpect(jsonPath("$.result.createdAt").value("2026-08-25T10:00:00"));

        verify(userQueryService).getMyProfile(7L);
    }

    @Test
    void Authorization_헤더가_없으면_로그인_필요로_응답한다() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("COMMON4011"));
    }

    @Test
    void 유효하지_않은_Access_Token은_거절한다() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH4012"));
    }
}
