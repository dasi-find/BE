package com.dasifind.backend.domain.searchcard.entity;

import com.dasifind.backend.domain.searchcard.model.SearchCardStatus;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "search_card")
public class SearchCard {

    private static final int DEFAULT_SEARCH_DAYS = 30;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "analysis_id", nullable = false, unique = true)
    private Long analysisId;

    @Column(name = "category", nullable = false, length = 50)
    private String category;

    @Column(name = "item_name", nullable = false, length = 100)
    private String itemName;

    @Column(name = "brand", length = 100)
    private String brand;

    @Column(name = "material", length = 100)
    private String material;

    @Column(name = "feature_description", nullable = false, columnDefinition = "TEXT")
    private String featureDescription;

    @Column(name = "lost_date", nullable = false)
    private LocalDate lostDate;

    @Column(name = "lost_start_time")
    private LocalTime lostStartTime;

    @Column(name = "lost_end_time")
    private LocalTime lostEndTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SearchCardStatus status;

    @Column(name = "search_expires_at", nullable = false)
    private LocalDateTime searchExpiresAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "search_card_color",
            joinColumns = @JoinColumn(name = "search_card_id")
    )
    @OrderColumn(name = "sort_order")
    @Column(name = "color", nullable = false, length = 50)
    private List<String> colors = new ArrayList<>();

    protected SearchCard() {
    }

    private SearchCard(
            Long userId,
            Long analysisId,
            String category,
            String itemName,
            List<String> colors,
            String brand,
            String material,
            String featureDescription,
            LocalDate lostDate,
            LocalTime lostStartTime,
            LocalTime lostEndTime,
            LocalDateTime now
    ) {
        this.userId = userId;
        this.analysisId = analysisId;
        this.category = category;
        this.itemName = itemName;
        this.colors.addAll(colors);
        this.brand = brand;
        this.material = material;
        this.featureDescription = featureDescription;
        this.lostDate = lostDate;
        this.lostStartTime = lostStartTime;
        this.lostEndTime = lostEndTime;
        this.status = SearchCardStatus.ACTIVE;
        this.searchExpiresAt = now.toLocalDate()
                .plusDays(DEFAULT_SEARCH_DAYS)
                .atTime(23, 59, 59);
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static SearchCard create(
            Long userId,
            Long analysisId,
            String category,
            String itemName,
            List<String> colors,
            String brand,
            String material,
            String featureDescription,
            LocalDate lostDate,
            LocalTime lostStartTime,
            LocalTime lostEndTime,
            LocalDateTime now
    ) {
        return new SearchCard(
                userId,
                analysisId,
                category,
                itemName,
                colors,
                brand,
                material,
                featureDescription,
                lostDate,
                lostStartTime,
                lostEndTime,
                now
        );
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getAnalysisId() {
        return analysisId;
    }

    public String getCategory() {
        return category;
    }

    public String getItemName() {
        return itemName;
    }

    public List<String> getColors() {
        return List.copyOf(colors);
    }

    public String getBrand() {
        return brand;
    }

    public String getMaterial() {
        return material;
    }

    public String getFeatureDescription() {
        return featureDescription;
    }

    public LocalDate getLostDate() {
        return lostDate;
    }

    public LocalTime getLostStartTime() {
        return lostStartTime;
    }

    public LocalTime getLostEndTime() {
        return lostEndTime;
    }

    public SearchCardStatus getStatus() {
        return status;
    }

    public LocalDateTime getSearchExpiresAt() {
        return searchExpiresAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
