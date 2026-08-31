package com.dasifind.backend.domain.user.dto.request;

public record UpdateMyProfileRequest(
        String name,
        Boolean emailNotificationEnabled
) {
}
