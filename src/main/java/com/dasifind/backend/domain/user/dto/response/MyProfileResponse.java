package com.dasifind.backend.domain.user.dto.response;

import com.dasifind.backend.domain.user.entity.User;

import java.time.LocalDateTime;

public record MyProfileResponse(
        Long id,
        String email,
        String name,
        boolean emailNotificationEnabled,
        LocalDateTime createdAt
) {

    public static MyProfileResponse from(User user) {
        return new MyProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.isEmailNotificationEnabled(),
                user.getCreatedAt()
        );
    }
}
