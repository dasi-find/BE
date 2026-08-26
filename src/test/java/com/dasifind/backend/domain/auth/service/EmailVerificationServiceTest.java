package com.dasifind.backend.domain.auth.service;

import com.dasifind.backend.domain.auth.config.EmailVerificationProperties;
import com.dasifind.backend.domain.auth.dto.response.EmailVerificationConfirmResponse;
import com.dasifind.backend.domain.auth.dto.response.EmailVerificationSendResponse;
import com.dasifind.backend.domain.auth.mail.VerificationEmailSender;
import com.dasifind.backend.domain.auth.model.EmailVerificationState;
import com.dasifind.backend.domain.auth.repository.EmailVerificationRepository;
import com.dasifind.backend.domain.user.repository.UserRepository;
import com.dasifind.backend.global.error.BusinessException;
import com.dasifind.backend.global.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    private static final EmailVerificationProperties PROPERTIES = new EmailVerificationProperties(
            Duration.ofMinutes(5),
            Duration.ofSeconds(60),
            5,
            Duration.ofMinutes(30)
    );

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailVerificationRepository verificationRepository;

    @Mock
    private VerificationEmailSender emailSender;

    private EmailVerificationService service;

    @BeforeEach
    void setUp() {
        service = new EmailVerificationService(
                userRepository,
                verificationRepository,
                emailSender,
                PROPERTIES,
                new SecureRandom()
        );
    }

    @Test
    void 이미_가입된_이메일에는_인증번호를_보내지_않는다() {
        when(userRepository.existsByEmailIgnoreCase("user@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.send(" USER@Example.com "))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS));

        verify(verificationRepository, never()).acquireSendCooldown(anyString(), any());
        verify(emailSender, never()).sendVerificationCode(anyString(), anyString(), eq(5L));
    }

    @Test
    void 재발송_대기_시간_중에는_요청을_제한한다() {
        when(verificationRepository.acquireSendCooldown(anyString(), eq(Duration.ofSeconds(60))))
                .thenReturn(false);

        assertThatThrownBy(() -> service.send("user@example.com"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RATE_LIMIT_EXCEEDED));
    }

    @Test
    void 인증번호를_안전하게_저장하고_이메일로_발송한다() {
        when(verificationRepository.acquireSendCooldown(anyString(), any())).thenReturn(true);

        EmailVerificationSendResponse response = service.send(" USER@Example.com ");

        ArgumentCaptor<String> emailKeyCaptor = ArgumentCaptor.forClass(String.class);
        verify(verificationRepository).acquireSendCooldown(emailKeyCaptor.capture(), eq(Duration.ofSeconds(60)));
        assertThat(emailKeyCaptor.getValue()).hasSize(64).doesNotContain("user@example.com");

        ArgumentCaptor<EmailVerificationState> stateCaptor = ArgumentCaptor.forClass(EmailVerificationState.class);
        verify(verificationRepository).saveRequest(
                eq(response.verificationId()),
                stateCaptor.capture(),
                eq(Duration.ofMinutes(5))
        );
        EmailVerificationState state = stateCaptor.getValue();
        assertThat(state.email()).isEqualTo("user@example.com");
        assertThat(state.codeHash()).hasSize(64);
        assertThat(state.attempts()).isZero();

        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailSender).sendVerificationCode(eq("user@example.com"), codeCaptor.capture(), eq(5L));
        assertThat(codeCaptor.getValue()).matches("\\d{6}");
        assertThat(state.codeHash()).doesNotContain(codeCaptor.getValue());
        assertThat(response.verificationId()).startsWith("ev_");
        assertThat(response.expiresInSeconds()).isEqualTo(300);
    }

    @Test
    void 메일_발송에_실패하면_저장된_요청과_재발송_제한을_되돌린다() {
        when(verificationRepository.acquireSendCooldown(anyString(), any())).thenReturn(true);
        doThrow(new IllegalStateException("mail failed"))
                .when(emailSender).sendVerificationCode(anyString(), anyString(), eq(5L));

        assertThatThrownBy(() -> service.send("user@example.com"))
                .isInstanceOf(IllegalStateException.class);

        ArgumentCaptor<String> idCaptor = ArgumentCaptor.forClass(String.class);
        verify(verificationRepository).saveRequest(idCaptor.capture(), any(), eq(Duration.ofMinutes(5)));
        verify(verificationRepository).deleteRequest(idCaptor.getValue());
        verify(verificationRepository).releaseSendCooldown(anyString());
    }

    @Test
    void 만료되거나_없는_인증_요청은_거절한다() {
        when(verificationRepository.isConfirmed("ev_expired")).thenReturn(false);
        when(verificationRepository.findRequest("ev_expired")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.confirm("ev_expired", "123456"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.EMAIL_VERIFICATION_EXPIRED));
    }

    @Test
    void 인증번호가_틀리면_시도_횟수를_올린다() {
        EmailVerificationState state = sentState("user@example.com");
        when(verificationRepository.findRequest(anyString())).thenReturn(Optional.of(state));
        when(verificationRepository.incrementAttempts(anyString())).thenReturn(1L);

        assertThatThrownBy(() -> service.confirm("ev_request", "999999"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_VERIFICATION_CODE));

        verify(verificationRepository).incrementAttempts("ev_request");
    }

    @Test
    void 다섯_번째_실패부터_추가_확인을_제한한다() {
        EmailVerificationState state = sentState("user@example.com");
        when(verificationRepository.findRequest(anyString())).thenReturn(Optional.of(state));
        when(verificationRepository.incrementAttempts(anyString())).thenReturn(5L);

        assertThatThrownBy(() -> service.confirm("ev_request", "999999"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RATE_LIMIT_EXCEEDED));
    }

    @Test
    void 올바른_인증번호를_확인하고_일회용_토큰을_발급한다() {
        when(verificationRepository.acquireSendCooldown(anyString(), any())).thenReturn(true);
        EmailVerificationSendResponse sendResponse = service.send("user@example.com");

        ArgumentCaptor<EmailVerificationState> stateCaptor = ArgumentCaptor.forClass(EmailVerificationState.class);
        verify(verificationRepository).saveRequest(anyString(), stateCaptor.capture(), any());
        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailSender).sendVerificationCode(anyString(), codeCaptor.capture(), any(Long.class));

        when(verificationRepository.findRequest(sendResponse.verificationId()))
                .thenReturn(Optional.of(stateCaptor.getValue()));
        when(verificationRepository.markConfirmed(sendResponse.verificationId(), Duration.ofMinutes(30)))
                .thenReturn(true);

        EmailVerificationConfirmResponse response = service.confirm(
                sendResponse.verificationId(),
                codeCaptor.getValue()
        );

        assertThat(response.verificationToken()).startsWith("evt_");
        assertThat(response.verifiedEmail()).isEqualTo("user@example.com");
        verify(verificationRepository).deleteRequest(sendResponse.verificationId());
        verify(verificationRepository).saveToken(
                response.verificationToken(),
                "user@example.com",
                Duration.ofMinutes(30)
        );
    }

    @Test
    void 인증_토큰을_한_번만_사용하고_이메일과_대조한다() {
        when(verificationRepository.consumeToken("evt_token", "user@example.com"))
                .thenReturn(true)
                .thenReturn(false);

        service.consumeVerificationToken("evt_token", " USER@example.com ");

        assertThatThrownBy(() -> service.consumeVerificationToken("evt_token", "user@example.com"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.EMAIL_VERIFICATION_EXPIRED));
    }

    @Test
    void 인증_토큰을_소비하지_않고_이메일과_일치하는지_검증한다() {
        when(verificationRepository.matchesToken("evt_token", "user@example.com")).thenReturn(true);

        service.validateVerificationToken("evt_token", " USER@example.com ");

        verify(verificationRepository).matchesToken("evt_token", "user@example.com");
        verify(verificationRepository, never()).consumeToken(anyString(), anyString());
    }

    @Test
    void 사전_검증에서_만료되거나_이메일이_다른_토큰을_거절한다() {
        when(verificationRepository.matchesToken("evt_token", "user@example.com")).thenReturn(false);

        assertThatThrownBy(() -> service.validateVerificationToken("evt_token", "user@example.com"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.EMAIL_VERIFICATION_EXPIRED));
    }

    @Test
    void 이미_확인한_요청을_다시_확인하면_중복_요청으로_거절한다() {
        when(verificationRepository.isConfirmed("ev_confirmed")).thenReturn(true);

        assertThatThrownBy(() -> service.confirm("ev_confirmed", "123456"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_REQUEST));

        verify(verificationRepository, never()).findRequest(anyString());
    }

    private EmailVerificationState sentState(String email) {
        when(verificationRepository.acquireSendCooldown(anyString(), any())).thenReturn(true);
        EmailVerificationSendResponse response = service.send(email);
        ArgumentCaptor<EmailVerificationState> stateCaptor = ArgumentCaptor.forClass(EmailVerificationState.class);
        verify(verificationRepository).saveRequest(eq(response.verificationId()), stateCaptor.capture(), any());
        return stateCaptor.getValue();
    }
}
