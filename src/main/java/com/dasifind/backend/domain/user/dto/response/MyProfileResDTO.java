package com.dasifind.backend.domain.user.dto.response;

import com.dasifind.backend.domain.user.entity.User;

import java.time.LocalDateTime;

public record MyProfileResDTO(
        Long id,
        String email,
        String name,
        boolean emailNotificationEnabled,
        LocalDateTime createdAt
) {

    public static MyProfileResDTO from(User user) {
        return new MyProfileResDTO(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.isEmailNotificationEnabled(),
                user.getCreatedAt()
        );
    }
}
