package com.dasifind.backend.domain.notification.controller;

import com.dasifind.backend.domain.notification.dto.response.NotificationListResDTO;
import com.dasifind.backend.domain.notification.dto.response.NotificationReadResDTO;
import com.dasifind.backend.domain.notification.dto.response.NotificationReadAllResDTO;
import com.dasifind.backend.domain.notification.dto.response.NotificationUnreadCountResDTO;
import com.dasifind.backend.domain.notification.service.NotificationService;
import com.dasifind.backend.global.api.ApiResDTO;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequestMapping("/api/v1/notifications")
public class NotificationController {
    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResDTO<NotificationListResDTO> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResDTO.success(service.list(Long.valueOf(jwt.getSubject()), unreadOnly, page, size));
    }

    @GetMapping("/unread-count")
    public ApiResDTO<NotificationUnreadCountResDTO> unreadCount(@AuthenticationPrincipal Jwt jwt) {
        return ApiResDTO.success(service.unreadCount(Long.valueOf(jwt.getSubject())));
    }

    @PostMapping("/{notificationId}/read")
    public ApiResDTO<NotificationReadResDTO> read(
            @AuthenticationPrincipal Jwt jwt, @PathVariable @Min(1) Long notificationId) {
        return ApiResDTO.success(service.read(Long.valueOf(jwt.getSubject()), notificationId));
    }

    @PostMapping("/read-all")
    public ApiResDTO<NotificationReadAllResDTO> readAll(@AuthenticationPrincipal Jwt jwt) {
        return ApiResDTO.success(service.readAll(Long.valueOf(jwt.getSubject())));
    }
}
