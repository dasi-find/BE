package com.dasifind.backend.domain.user.dto.response;

import com.dasifind.backend.domain.user.entity.User;

public record UpdateMyProfileResDTO(
        Long id,
        String email,
        String name,
        boolean emailNotificationEnabled
) {

    public static UpdateMyProfileResDTO from(User user) {
        return new UpdateMyProfileResDTO(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.isEmailNotificationEnabled()
        );
    }
}
