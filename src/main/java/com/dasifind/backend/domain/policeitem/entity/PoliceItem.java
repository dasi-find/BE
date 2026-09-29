package com.dasifind.backend.domain.policeitem.entity;

import com.dasifind.backend.domain.policeitem.model.PoliceItemDetails;
import com.dasifind.backend.domain.policeitem.model.PoliceItemSource;
import jakarta.persistence.*;
import lombok.Getter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@Entity
@Table(name = "police_item", uniqueConstraints = @UniqueConstraint(
        name = "uk_police_item_source_key", columnNames = {"source", "management_no", "item_sequence"}))
public class PoliceItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PoliceItemSource source;
    @Column(name = "management_no", nullable = false, length = 100)
    private String managementNo;
    @Column(name = "item_sequence", nullable = false)
    private int itemSequence;
    @Column(name = "item_name", nullable = false, length = 200)
    private String itemName;
    @Column(length = 100)
    private String category;
    @Column(length = 100)
    private String color;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Column(name = "found_date")
    private LocalDate foundDate;
    @Column(name = "found_place", length = 500)
    private String foundPlace;
    @Column(name = "storage_place", length = 200)
    private String storagePlace;
    @Column(name = "image_url", length = 2048)
    private String imageUrl;
    @Column(name = "original_url", length = 2048)
    private String originalUrl;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    @Version
    private long version;

    protected PoliceItem() {
    }

    public static PoliceItem create(PoliceItemSource source, String managementNo, int itemSequence,
                                    PoliceItemDetails details, LocalDateTime now) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(now, "now");
        if (managementNo == null || managementNo.isBlank() || managementNo.length() > 100
                || !managementNo.equals(managementNo.strip()) || itemSequence < 1) {
            throw new IllegalArgumentException("Normalized management number and positive sequence required");
        }
        PoliceItem item = new PoliceItem();
        item.source = source;
        item.managementNo = managementNo;
        item.itemSequence = itemSequence;
        item.createdAt = now;
        item.updateDetails(details, now);
        return item;
    }

    public void updateDetails(PoliceItemDetails details, LocalDateTime now) {
        Objects.requireNonNull(details, "details");
        Objects.requireNonNull(now, "now");
        if (now.isBefore(createdAt) || (updatedAt != null && now.isBefore(updatedAt))) {
            throw new IllegalArgumentException("Update time cannot move backwards");
        }
        itemName = details.itemName();
        category = details.category();
        color = details.color();
        description = details.description();
        foundDate = details.foundDate();
        foundPlace = details.foundPlace();
        storagePlace = details.storagePlace();
        imageUrl = details.imageUrl();
        originalUrl = details.originalUrl();
        updatedAt = now;
    }
}
