package com.dasifind.backend.domain.auth.service;

import com.dasifind.backend.domain.auth.dto.request.LoginRequest;
import com.dasifind.backend.domain.auth.model.IssuedTokens;
import com.dasifind.backend.domain.auth.model.LoginResult;
import com.dasifind.backend.domain.user.entity.User;
import com.dasifind.backend.domain.user.repository.UserRepository;
import com.dasifind.backend.global.error.BusinessException;
import com.dasifind.backend.global.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private LoginAttemptService loginAttemptService;

    @Mock
    private AuthTokenService authTokenService;

    private LoginService loginService;

    @BeforeEach
    void setUp() {
        when(passwordEncoder.encode("dummy-password-for-timing-protection"))
                .thenReturn("dummy-password-hash");
        loginService = new LoginService(
                userRepository,
                passwordEncoder,
                loginAttemptService,
                authTokenService
        );
    }

    @Test
    void 이메일과_비밀번호가_일치하면_실패_횟수를_초기화하고_토큰을_발급한다() {
        User user = user(7L, "user@example.com", "encoded-password");
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encoded-password")).thenReturn(true);
        when(authTokenService.issue(7L))
                .thenReturn(new IssuedTokens("access-token", 1800, "refresh-token"));

        LoginResult result = loginService.login(new LoginRequest(" USER@example.com ", "password123"));

        InOrder inOrder = inOrder(loginAttemptService, authTokenService);
        inOrder.verify(loginAttemptService).ensureAllowed("user@example.com");
        inOrder.verify(authTokenService).issue(7L);
        inOrder.verify(loginAttemptService).clear("user@example.com");
        assertThat(result.response().user().id()).isEqualTo(7L);
        assertThat(result.response().accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
    }

    @Test
    void 토큰_발급에_실패하면_로그인_실패_횟수를_초기화하지_않는다() {
        User user = user(7L, "user@example.com", "encoded-password");
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encoded-password")).thenReturn(true);
        RuntimeException tokenIssueFailure = new RuntimeException("token issue failed");
        when(authTokenService.issue(7L)).thenThrow(tokenIssueFailure);

        assertThatThrownBy(() -> loginService.login(new LoginRequest("user@example.com", "password123")))
                .isSameAs(tokenIssueFailure);

        verify(loginAttemptService, never()).clear("user@example.com");
    }

    @Test
    void 비밀번호가_틀리면_실패_횟수를_올리고_공통_인증_오류를_반환한다() {
        User user = user(7L, "user@example.com", "encoded-password");
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

        assertThatThrownBy(() -> loginService.login(new LoginRequest("user@example.com", "wrong-password")))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS));

        verify(loginAttemptService).recordFailure("user@example.com");
        verify(authTokenService, never()).issue(7L);
    }

    @Test
    void 존재하지_않는_이메일도_더미_해시로_비밀번호를_비교하고_같은_오류를_반환한다() {
        when(userRepository.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.matches("password123", "dummy-password-hash")).thenReturn(false);

        assertThatThrownBy(() -> loginService.login(new LoginRequest("missing@example.com", "password123")))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS));

        verify(passwordEncoder).matches("password123", "dummy-password-hash");
        verify(loginAttemptService).recordFailure("missing@example.com");
    }

    @Test
    void 차단된_이메일은_사용자_조회와_비밀번호_검증을_시도하지_않는다() {
        BusinessException rateLimited = new BusinessException(ErrorCode.RATE_LIMIT_EXCEEDED);
        org.mockito.Mockito.doThrow(rateLimited)
                .when(loginAttemptService).ensureAllowed("user@example.com");

        assertThatThrownBy(() -> loginService.login(new LoginRequest("user@example.com", "password123")))
                .isSameAs(rateLimited);

        verify(userRepository, never()).findByEmailIgnoreCase(anyString());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    private User user(Long id, String email, String encodedPassword) {
        User user = User.create(email, encodedPassword, "민준", true);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
