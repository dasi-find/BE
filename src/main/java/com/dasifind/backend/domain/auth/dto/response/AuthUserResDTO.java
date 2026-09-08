package com.dasifind.backend.domain.auth.dto.response;

import com.dasifind.backend.domain.user.entity.User;

public record AuthUserResDTO(
        Long id,
        String email,
        String name
) {

    public static AuthUserResDTO from(User user) {
        return new AuthUserResDTO(user.getId(), user.getEmail(), user.getName());
    }
}
