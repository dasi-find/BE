package com.dasifind.backend.domain.searchcard.image.entity;

import com.dasifind.backend.domain.searchcard.image.model.SearchCardImageType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "search_card_image")
public class SearchCardImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "search_card_id")
    private Long searchCardId;

    @Column(name = "storage_key", nullable = false, unique = true, length = 500)
    private String storageKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "image_type", nullable = false, length = 20)
    private SearchCardImageType imageType;

    @Column(name = "content_type", nullable = false, length = 50)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected SearchCardImage() {
    }

    private SearchCardImage(
            Long userId,
            String storageKey,
            SearchCardImageType imageType,
            String contentType,
            long fileSize,
            LocalDateTime createdAt
    ) {
        this.userId = userId;
        this.storageKey = storageKey;
        this.imageType = imageType;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.createdAt = createdAt;
    }

    public static SearchCardImage create(
            Long userId,
            String storageKey,
            SearchCardImageType imageType,
            String contentType,
            long fileSize
    ) {
        return new SearchCardImage(
                userId,
                storageKey,
                imageType,
                contentType,
                fileSize,
                LocalDateTime.now()
        );
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getSearchCardId() {
        return searchCardId;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public SearchCardImageType getImageType() {
        return imageType;
    }

    public String getContentType() {
        return contentType;
    }

    public long getFileSize() {
        return fileSize;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void attachTo(Long searchCardId) {
        if (this.searchCardId != null) {
            throw new IllegalStateException("이미 수색카드에 연결된 이미지입니다.");
        }
        this.searchCardId = searchCardId;
    }
}
