package com.dasifind.backend.domain.searchcard.analysis.entity;

import com.dasifind.backend.domain.searchcard.analysis.client.AiAnalysisClientResponse;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "search_card_analysis")
public class SearchCardAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "category", nullable = false, length = 50)
    private String category;

    @Column(name = "item_name", nullable = false, length = 100)
    private String itemName;

    @Column(name = "brand", length = 100)
    private String brand;

    @Column(name = "ocr_text", length = 1000)
    private String ocrText;

    @Column(name = "model_version", nullable = false, length = 100)
    private String modelVersion;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "search_card_analysis_color",
            joinColumns = @JoinColumn(name = "analysis_id")
    )
    @OrderColumn(name = "sort_order")
    @Column(name = "color", nullable = false, length = 50)
    private List<String> colors = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "search_card_analysis_material",
            joinColumns = @JoinColumn(name = "analysis_id")
    )
    @OrderColumn(name = "sort_order")
    @Column(name = "material", nullable = false, length = 50)
    private List<String> materials = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "search_card_analysis_feature",
            joinColumns = @JoinColumn(name = "analysis_id")
    )
    @OrderColumn(name = "sort_order")
    @Column(name = "feature", nullable = false, length = 500)
    private List<String> features = new ArrayList<>();

    protected SearchCardAnalysis() {
    }

    private SearchCardAnalysis(Long userId, AiAnalysisClientResponse result) {
        this.userId = userId;
        this.category = result.category();
        this.itemName = result.itemName();
        this.brand = result.brand();
        this.ocrText = result.ocrText();
        this.modelVersion = result.modelVersion();
        this.createdAt = LocalDateTime.now();
        this.colors.addAll(result.colors());
        this.materials.addAll(result.materials());
        this.features.addAll(result.features());
    }

    public static SearchCardAnalysis create(Long userId, AiAnalysisClientResponse result) {
        return new SearchCardAnalysis(userId, result);
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getCategory() {
        return category;
    }

    public String getItemName() {
        return itemName;
    }

    public String getBrand() {
        return brand;
    }

    public String getOcrText() {
        return ocrText;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<String> getColors() {
        return colors;
    }

    public List<String> getMaterials() {
        return materials;
    }

    public List<String> getFeatures() {
        return features;
    }
}
