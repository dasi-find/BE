package com.dasifind.backend.domain.user.dto.request;

public record UpdateMyProfileReqDTO(
        String name,
        Boolean emailNotificationEnabled
) {
}
