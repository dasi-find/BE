package com.dasifind.backend.domain.user.service;

import com.dasifind.backend.domain.user.dto.request.UpdateMyProfileRequest;
import com.dasifind.backend.domain.user.dto.response.UpdateMyProfileResponse;
import com.dasifind.backend.domain.user.entity.User;
import com.dasifind.backend.domain.user.repository.UserRepository;
import com.dasifind.backend.global.error.BusinessException;
import com.dasifind.backend.global.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserCommandServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserCommandService userCommandService;

    @Test
    void 표시명과_이메일_알림_설정을_함께_수정한다() {
        User user = user();
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        UpdateMyProfileResponse response = userCommandService.updateMyProfile(
                7L,
                new UpdateMyProfileRequest("  새 이름  ", false)
        );

        assertThat(response.name()).isEqualTo("새 이름");
        assertThat(response.emailNotificationEnabled()).isFalse();
        assertThat(user.getName()).isEqualTo("새 이름");
        assertThat(user.isEmailNotificationEnabled()).isFalse();
    }

    @Test
    void 표시명만_전달하면_알림_설정은_유지한다() {
        User user = user();
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        UpdateMyProfileResponse response = userCommandService.updateMyProfile(
                7L,
                new UpdateMyProfileRequest("새 이름", null)
        );

        assertThat(response.name()).isEqualTo("새 이름");
        assertThat(response.emailNotificationEnabled()).isTrue();
    }

    @Test
    void 알림_설정만_전달하면_표시명은_유지한다() {
        User user = user();
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        UpdateMyProfileResponse response = userCommandService.updateMyProfile(
                7L,
                new UpdateMyProfileRequest(null, false)
        );

        assertThat(response.name()).isEqualTo("민준");
        assertThat(response.emailNotificationEnabled()).isFalse();
    }

    @Test
    void 수정할_필드가_없으면_잘못된_요청으로_처리한다() {
        assertInvalidRequest(new UpdateMyProfileRequest(null, null));
        verify(userRepository, never()).findById(7L);
    }

    @Test
    void 표시명이_공백이면_잘못된_요청으로_처리한다() {
        assertInvalidRequest(new UpdateMyProfileRequest("   ", null));
        verify(userRepository, never()).findById(7L);
    }

    @Test
    void 공백을_제거한_표시명이_50자를_초과하면_잘못된_요청으로_처리한다() {
        assertInvalidRequest(new UpdateMyProfileRequest("가".repeat(51), null));
        verify(userRepository, never()).findById(7L);
    }

    @Test
    void 토큰의_사용자가_존재하지_않으면_유효하지_않은_토큰으로_처리한다() {
        when(userRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userCommandService.updateMyProfile(
                7L,
                new UpdateMyProfileRequest("새 이름", null)
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_TOKEN));
    }

    private void assertInvalidRequest(UpdateMyProfileRequest request) {
        assertThatThrownBy(() -> userCommandService.updateMyProfile(7L, request))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
    }

    private User user() {
        User user = User.create("hello@dasifind.kr", "encoded-password", "민준", true);
        ReflectionTestUtils.setField(user, "id", 7L);
        return user;
    }
}
