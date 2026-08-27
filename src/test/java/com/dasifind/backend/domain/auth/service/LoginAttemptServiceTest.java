package com.dasifind.backend.domain.auth.service;

import com.dasifind.backend.domain.auth.config.LoginAttemptProperties;
import com.dasifind.backend.domain.auth.repository.LoginAttemptRepository;
import com.dasifind.backend.global.error.BusinessException;
import com.dasifind.backend.global.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginAttemptServiceTest {

    private static final Duration BLOCK_DURATION = Duration.ofMinutes(10);

    @Mock
    private LoginAttemptRepository loginAttemptRepository;

    private LoginAttemptService loginAttemptService;

    @BeforeEach
    void setUp() {
        loginAttemptService = new LoginAttemptService(
                loginAttemptRepository,
                new LoginAttemptProperties(5, BLOCK_DURATION)
        );
    }

    @Test
    void 네_번까지의_실패는_추가_로그인을_허용한다() {
        when(loginAttemptRepository.getAttempts(anyString())).thenReturn(4L);

        assertThatCode(() -> loginAttemptService.ensureAllowed("user@example.com"))
                .doesNotThrowAnyException();
    }

    @Test
    void 다섯_번_실패한_이메일은_십_분간_차단한다() {
        when(loginAttemptRepository.getAttempts(anyString())).thenReturn(5L);

        assertThatThrownBy(() -> loginAttemptService.ensureAllowed("user@example.com"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RATE_LIMIT_EXCEEDED));
    }

    @Test
    void 이메일_원문_대신_해시로_실패_횟수를_저장한다() {
        loginAttemptService.recordFailure("user@example.com");

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(loginAttemptRepository).incrementAttempts(keyCaptor.capture(),
                org.mockito.ArgumentMatchers.eq(BLOCK_DURATION));
        assertThat(keyCaptor.getValue())
                .hasSize(64)
                .matches("[0-9a-f]{64}")
                .doesNotContain("user@example.com");
    }

    @Test
    void 로그인_성공_시_해시된_이메일의_실패_횟수를_초기화한다() {
        loginAttemptService.clear("user@example.com");

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(loginAttemptRepository).clear(keyCaptor.capture());
        assertThat(keyCaptor.getValue()).hasSize(64).matches("[0-9a-f]{64}");
    }
}
