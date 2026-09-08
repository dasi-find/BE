package com.dasifind.backend.domain.searchcard.image.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "search_card_image_deletion_task")
public class SearchCardImageDeletionTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "storage_key", nullable = false, unique = true, length = 500)
    private String storageKey;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected SearchCardImageDeletionTask() {
    }

    private SearchCardImageDeletionTask(String storageKey, LocalDateTime createdAt) {
        this.storageKey = storageKey;
        this.createdAt = createdAt;
    }

    public static SearchCardImageDeletionTask create(String storageKey) {
        return new SearchCardImageDeletionTask(storageKey, LocalDateTime.now());
    }

    public Long getId() {
        return id;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
