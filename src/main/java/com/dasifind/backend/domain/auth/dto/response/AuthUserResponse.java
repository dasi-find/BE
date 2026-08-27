package com.dasifind.backend.domain.auth.dto.response;

import com.dasifind.backend.domain.user.entity.User;

public record AuthUserResponse(
        Long id,
        String email,
        String name
) {

    public static AuthUserResponse from(User user) {
        return new AuthUserResponse(user.getId(), user.getEmail(), user.getName());
    }
}
