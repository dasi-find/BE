package com.dasifind.backend.domain.user.service;

import com.dasifind.backend.domain.user.dto.response.MyProfileResponse;
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

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserQueryServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserQueryService userQueryService;

    @Test
    void 사용자_ID로_내_정보를_조회한다() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 25, 10, 0);
        User user = User.create("hello@dasifind.kr", "encoded-password", "민준", true);
        ReflectionTestUtils.setField(user, "id", 7L);
        ReflectionTestUtils.setField(user, "createdAt", createdAt);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        MyProfileResponse response = userQueryService.getMyProfile(7L);

        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.email()).isEqualTo("hello@dasifind.kr");
        assertThat(response.name()).isEqualTo("민준");
        assertThat(response.emailNotificationEnabled()).isTrue();
        assertThat(response.createdAt()).isEqualTo(createdAt);
    }

    @Test
    void 토큰의_사용자가_존재하지_않으면_유효하지_않은_토큰으로_처리한다() {
        when(userRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userQueryService.getMyProfile(7L))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_TOKEN));
    }
}
