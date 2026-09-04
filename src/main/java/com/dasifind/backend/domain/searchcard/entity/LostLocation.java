package com.dasifind.backend.domain.searchcard.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "lost_location")
public class LostLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "search_card_id", nullable = false, unique = true)
    private Long searchCardId;

    @Column(name = "place_name", nullable = false, length = 100)
    private String placeName;

    @Column(name = "address", nullable = false, length = 255)
    private String address;

    @Column(name = "latitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected LostLocation() {
    }

    private LostLocation(
            Long searchCardId,
            String placeName,
            String address,
            BigDecimal latitude,
            BigDecimal longitude,
            String description,
            LocalDateTime now
    ) {
        this.searchCardId = searchCardId;
        this.placeName = placeName;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.description = description;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static LostLocation create(
            Long searchCardId,
            String placeName,
            String address,
            BigDecimal latitude,
            BigDecimal longitude,
            String description,
            LocalDateTime now
    ) {
        return new LostLocation(
                searchCardId,
                placeName,
                address,
                latitude,
                longitude,
                description,
                now
        );
    }

    public Long getId() {
        return id;
    }

    public Long getSearchCardId() {
        return searchCardId;
    }

    public String getPlaceName() {
        return placeName;
    }

    public String getAddress() {
        return address;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public String getDescription() {
        return description;
    }
}
