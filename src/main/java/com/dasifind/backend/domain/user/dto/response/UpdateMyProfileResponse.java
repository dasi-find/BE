package com.dasifind.backend.domain.user.dto.response;

import com.dasifind.backend.domain.user.entity.User;

public record UpdateMyProfileResponse(
        Long id,
        String email,
        String name,
        boolean emailNotificationEnabled
) {

    public static UpdateMyProfileResponse from(User user) {
        return new UpdateMyProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.isEmailNotificationEnabled()
        );
    }
}
