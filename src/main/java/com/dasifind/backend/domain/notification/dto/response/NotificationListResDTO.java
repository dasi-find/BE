package com.dasifind.backend.domain.notification.dto.response;

import com.dasifind.backend.domain.notification.entity.Notification;
import com.dasifind.backend.domain.notification.model.NotificationType;
import com.dasifind.backend.domain.notification.model.NotificationReferenceType;
import org.springframework.data.domain.Page;
import java.time.LocalDateTime;
import java.util.List;

public record NotificationListResDTO(List<NotificationItemResDTO> content, int page, int size,
                                     long totalElements, boolean hasNext) {
    public static NotificationListResDTO from(Page<Notification> page) {
        return new NotificationListResDTO(page.stream().map(NotificationItemResDTO::from).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.hasNext());
    }

    public record NotificationItemResDTO(Long id, NotificationType type, String title, String message,
                                         NotificationReferenceType referenceType, Long referenceId,
                                         boolean isRead, LocalDateTime createdAt) {
        static NotificationItemResDTO from(Notification notification) {
            return new NotificationItemResDTO(notification.getId(), notification.getType(), notification.getTitle(),
                    notification.getMessage(), notification.getReferenceType(), notification.getReferenceId(),
                    notification.isRead(), notification.getCreatedAt());
        }
    }
}
