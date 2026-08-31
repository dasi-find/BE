package com.dasifind.backend.domain.user.controller;

import com.dasifind.backend.domain.user.dto.request.UpdateMyProfileRequest;
import com.dasifind.backend.domain.user.dto.response.MyProfileResponse;
import com.dasifind.backend.domain.user.dto.response.UpdateMyProfileResponse;
import com.dasifind.backend.domain.user.service.UserCommandService;
import com.dasifind.backend.domain.user.service.UserQueryService;
import com.dasifind.backend.global.error.BusinessException;
import com.dasifind.backend.global.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

    @MockitoBean
    private UserCommandService userCommandService;

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

    @Test
    void 내_정보를_부분_수정한다() throws Exception {
        when(userCommandService.updateMyProfile(any(), any())).thenReturn(new UpdateMyProfileResponse(
                7L,
                "hello@dasifind.kr",
                "민준",
                false
        ));

        mockMvc.perform(patch("/api/v1/users/me")
                        .with(jwt().jwt(jwt -> jwt.subject("7").claim("tokenType", "access")))
                        .contentType("application/json")
                        .content("""
                                {
                                  "emailNotificationEnabled": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("COMMON2001"))
                .andExpect(jsonPath("$.result.id").value(7))
                .andExpect(jsonPath("$.result.email").value("hello@dasifind.kr"))
                .andExpect(jsonPath("$.result.name").value("민준"))
                .andExpect(jsonPath("$.result.emailNotificationEnabled").value(false));

        verify(userCommandService).updateMyProfile(
                7L,
                new UpdateMyProfileRequest(null, false)
        );
    }

    @Test
    void 표시명이_50자를_초과하면_잘못된_요청으로_응답한다() throws Exception {
        String tooLongName = "가".repeat(51);
        doThrow(new BusinessException(ErrorCode.INVALID_REQUEST))
                .when(userCommandService)
                .updateMyProfile(7L, new UpdateMyProfileRequest(tooLongName, null));

        mockMvc.perform(patch("/api/v1/users/me")
                        .with(jwt().jwt(jwt -> jwt.subject("7").claim("tokenType", "access")))
                        .contentType("application/json")
                        .content("{\"name\":\"" + tooLongName + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON4001"));
    }

    @Test
    void 수정할_필드가_없으면_잘못된_요청으로_응답한다() throws Exception {
        doThrow(new BusinessException(ErrorCode.INVALID_REQUEST))
                .when(userCommandService).updateMyProfile(7L, new UpdateMyProfileRequest(null, null));

        mockMvc.perform(patch("/api/v1/users/me")
                        .with(jwt().jwt(jwt -> jwt.subject("7").claim("tokenType", "access")))
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON4001"));
    }

    @Test
    void 내_정보_수정도_인증이_필요하다() throws Exception {
        mockMvc.perform(patch("/api/v1/users/me")
                        .contentType("application/json")
                        .content("{\"name\":\"새 이름\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("COMMON4011"));
    }
}
