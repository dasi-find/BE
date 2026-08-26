package com.dasifind.backend.domain.auth.service;

import com.dasifind.backend.domain.auth.dto.request.SignupAgreementsRequest;
import com.dasifind.backend.domain.auth.dto.request.SignupRequest;
import com.dasifind.backend.domain.auth.model.IssuedTokens;
import com.dasifind.backend.domain.auth.model.SignupResult;
import com.dasifind.backend.domain.user.entity.User;
import com.dasifind.backend.domain.user.repository.UserRepository;
import com.dasifind.backend.global.error.BusinessException;
import com.dasifind.backend.global.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SignupServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailVerificationService emailVerificationService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthTokenService authTokenService;

    private SignupService signupService;

    @BeforeEach
    void setUp() {
        signupService = new SignupService(
                userRepository,
                emailVerificationService,
                passwordEncoder,
                authTokenService
        );
    }

    @Test
    void 인증된_이메일로_사용자를_생성하고_토큰을_발급한다() {
        SignupRequest request = signupRequest(" USER@Example.com ");
        when(userRepository.existsByEmailIgnoreCase("user@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            ReflectionTestUtils.setField(user, "id", 7L);
            return user;
        });
        when(authTokenService.issue(7L))
                .thenReturn(new IssuedTokens("access-token", 1800, "refresh-token"));

        SignupResult result = signupService.signup(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getEmail()).isEqualTo("user@example.com");
        assertThat(savedUser.getPassword()).isEqualTo("encoded-password");
        assertThat(savedUser.getName()).isEqualTo("민준");
        assertThat(savedUser.isEmailNotificationEnabled()).isTrue();
        verify(emailVerificationService).consumeVerificationToken("evt_token", "user@example.com");
        assertThat(result.response().user().id()).isEqualTo(7L);
        assertThat(result.response().accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
    }

    @Test
    void 이미_가입된_이메일이면_회원가입을_거부한다() {
        SignupRequest request = signupRequest("user@example.com");
        when(userRepository.existsByEmailIgnoreCase("user@example.com")).thenReturn(true);

        assertThatThrownBy(() -> signupService.signup(request))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS));

        verify(userRepository, never()).saveAndFlush(any());
        verify(emailVerificationService, never()).consumeVerificationToken(any(), any());
    }

    @Test
    void 데이터베이스_중복_충돌도_가입된_이메일_오류로_변환한다() {
        SignupRequest request = signupRequest("user@example.com");
        when(userRepository.existsByEmailIgnoreCase("user@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userRepository.saveAndFlush(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate email"));

        assertThatThrownBy(() -> signupService.signup(request))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS));

        verify(emailVerificationService, never()).consumeVerificationToken(any(), any());
        verify(authTokenService, never()).issue(any());
    }

    private SignupRequest signupRequest(String email) {
        return new SignupRequest(
                email,
                "evt_token",
                "password123",
                " 민준 ",
                new SignupAgreementsRequest(true, true, true)
        );
    }
}
