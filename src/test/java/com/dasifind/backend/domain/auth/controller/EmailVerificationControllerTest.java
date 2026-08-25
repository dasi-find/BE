package com.dasifind.backend.domain.auth.controller;

import com.dasifind.backend.domain.auth.dto.response.EmailVerificationConfirmResponse;
import com.dasifind.backend.domain.auth.dto.response.EmailVerificationSendResponse;
import com.dasifind.backend.domain.auth.service.EmailVerificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EmailVerificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EmailVerificationService emailVerificationService;

    @Test
    void 인증번호_발송_API가_명세된_응답을_반환한다() throws Exception {
        when(emailVerificationService.send("user@example.com"))
                .thenReturn(new EmailVerificationSendResponse("ev_request", 300));

        mockMvc.perform(post("/api/v1/auth/email-verifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"user@example.com"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("COMMON2001"))
                .andExpect(jsonPath("$.result.verificationId").value("ev_request"))
                .andExpect(jsonPath("$.result.expiresInSeconds").value(300));
    }

    @Test
    void 인증번호_확인_API가_명세된_응답을_반환한다() throws Exception {
        when(emailVerificationService.confirm("ev_request", "123456"))
                .thenReturn(new EmailVerificationConfirmResponse("evt_token", "user@example.com"));

        mockMvc.perform(post("/api/v1/auth/email-verifications/ev_request/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"verificationCode":"123456"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result.verificationToken").value("evt_token"))
                .andExpect(jsonPath("$.result.verifiedEmail").value("user@example.com"));
    }

    @Test
    void 이메일_형식이_잘못되면_공통_오류_응답을_반환한다() throws Exception {
        mockMvc.perform(post("/api/v1/auth/email-verifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"invalid-email"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON4001"))
                .andExpect(jsonPath("$.result").doesNotExist());
    }

    @Test
    void 인증번호가_여섯_자리가_아니면_공통_오류_응답을_반환한다() throws Exception {
        mockMvc.perform(post("/api/v1/auth/email-verifications/ev_request/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"verificationCode":"12345"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON4001"));
    }
}
