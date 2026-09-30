package com.dasifind.backend.domain.notification.service;

import com.dasifind.backend.domain.notification.dto.response.NotificationListResDTO;
import com.dasifind.backend.domain.notification.dto.response.NotificationReadResDTO;
import com.dasifind.backend.domain.notification.dto.response.NotificationReadAllResDTO;
import com.dasifind.backend.domain.notification.dto.response.NotificationUnreadCountResDTO;
import com.dasifind.backend.domain.notification.repository.NotificationRepository;
import com.dasifind.backend.domain.user.repository.UserRepository;
import com.dasifind.backend.global.error.BusinessException;
import com.dasifind.backend.global.error.ErrorCode;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
@Transactional(readOnly = true)
public class NotificationService {
    private final NotificationRepository notifications;
    private final UserRepository users;
    private final Clock clock;

    public NotificationService(NotificationRepository notifications, UserRepository users, Clock clock) {
        this.notifications = notifications;
        this.users = users;
        this.clock = clock;
    }

    public NotificationListResDTO list(Long userId, boolean unreadOnly, int page, int size) {
        validateUser(userId);
        if (page < 0 || size < 1 || size > 100 || (long) page * size > Integer.MAX_VALUE) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        var result = unreadOnly ? notifications.findByUserIdAndReadAtIsNull(userId, pageable)
                : notifications.findByUserId(userId, pageable);
        return NotificationListResDTO.from(result);
    }

    public NotificationUnreadCountResDTO unreadCount(Long userId) {
        validateUser(userId);
        return new NotificationUnreadCountResDTO(notifications.countByUserIdAndReadAtIsNull(userId));
    }

    @Transactional
    public NotificationReadResDTO read(Long userId, Long id) {
        validateUser(userId);
        var notification = notifications.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (!notification.getUser().getId().equals(userId)) throw new BusinessException(ErrorCode.FORBIDDEN);
        notifications.markRead(userId, id, now());
        return new NotificationReadResDTO(id, true);
    }

    @Transactional
    public NotificationReadAllResDTO readAll(Long userId) {
        validateUser(userId);
        return new NotificationReadAllResDTO(notifications.markAllRead(userId, now()));
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.MICROS);
    }

    private void validateUser(Long userId) {
        if (!users.existsById(userId)) throw new BusinessException(ErrorCode.INVALID_TOKEN);
    }
}
