package com.dasifind.backend.domain.notification.entity;

import com.dasifind.backend.domain.candidate.entity.Candidate;
import com.dasifind.backend.domain.searchcard.entity.SearchCard;
import com.dasifind.backend.domain.user.entity.User;
import com.dasifind.backend.domain.notification.model.NotificationType;
import com.dasifind.backend.domain.notification.model.NotificationReferenceType;
import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@Entity
@Table(name = "notification")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Candidate candidate;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "search_card_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private SearchCard searchCard;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;
    @Column(nullable = false, length = 200)
    private String title;
    @Column(nullable = false, length = 2000)
    private String message;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "read_at")
    private LocalDateTime readAt;

    protected Notification() {}

    public static Notification forCandidate(User user, Candidate candidate, String title, String message, LocalDateTime now) {
        Objects.requireNonNull(candidate, "candidate");
        validateOwner(user, candidate.getSearchCard().getUserId());
        if (candidate.getId() == null) throw new IllegalArgumentException("Persist the candidate first");
        Notification notification = create(user, NotificationType.NEW_CANDIDATE, title, message, now);
        notification.candidate = candidate;
        return notification;
    }

    public static Notification forSearchCard(User user, SearchCard card, NotificationType type,
                                             String title, String message, LocalDateTime now) {
        Objects.requireNonNull(card, "card");
        validateOwner(user, card.getUserId());
        if (card.getId() == null || (type != NotificationType.SEARCH_EXPIRING && type != NotificationType.SEARCH_EXPIRED)) {
            throw new IllegalArgumentException("A persisted card and expiration notification type are required");
        }
        Notification notification = create(user, type, title, message, now);
        notification.searchCard = card;
        return notification;
    }

    public static Notification system(User user, String title, String message, LocalDateTime now) {
        return create(user, NotificationType.SYSTEM, title, message, now);
    }

    private static Notification create(User user, NotificationType type, String title, String message, LocalDateTime now) {
        Objects.requireNonNull(user, "user");
        if (user.getId() == null) throw new IllegalArgumentException("Persist the recipient first");
        validateText(title, 200);
        validateText(message, 2000);
        Notification notification = new Notification();
        notification.user = user;
        notification.type = type;
        notification.title = title;
        notification.message = message;
        notification.createdAt = Objects.requireNonNull(now, "now");
        return notification;
    }

    private static void validateOwner(User user, Long ownerId) {
        Objects.requireNonNull(user, "user");
        if (user.getId() == null || !user.getId().equals(ownerId)) {
            throw new IllegalArgumentException("Notification reference must belong to the recipient");
        }
    }

    private static void validateText(String value, int max) {
        if (value == null || value.isBlank() || value.length() > max) {
            throw new IllegalArgumentException("Notification text is missing or too long");
        }
    }

    public boolean isRead() { return readAt != null; }

    public NotificationReferenceType getReferenceType() {
        if (candidate != null) return NotificationReferenceType.CANDIDATE;
        if (searchCard != null) return NotificationReferenceType.SEARCH_CARD;
        return null;
    }

    public Long getReferenceId() {
        if (candidate != null) return candidate.getId();
        if (searchCard != null) return searchCard.getId();
        return null;
    }
}
