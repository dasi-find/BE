package com.dasifind.backend.domain.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "`user`")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "email_notification_enabled", nullable = false)
    private boolean emailNotificationEnabled;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected User() {
    }

    private User(
            String email,
            String password,
            String name,
            boolean emailNotificationEnabled,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.emailNotificationEnabled = emailNotificationEnabled;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static User create(
            String email,
            String password,
            String name,
            boolean emailNotificationEnabled
    ) {
        LocalDateTime now = LocalDateTime.now();
        return new User(email, password, name, emailNotificationEnabled, now, now);
    }

    public void updateProfile(String name, Boolean emailNotificationEnabled) {
        if (name != null) {
            this.name = name;
        }
        if (emailNotificationEnabled != null) {
            this.emailNotificationEnabled = emailNotificationEnabled;
        }
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getName() {
        return name;
    }

    public boolean isEmailNotificationEnabled() {
        return emailNotificationEnabled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
