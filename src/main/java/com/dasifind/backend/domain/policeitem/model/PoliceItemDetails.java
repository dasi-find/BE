package com.dasifind.backend.domain.policeitem.model;

import java.time.LocalDate;

/** Normalized display data, not the external API response. */
public record PoliceItemDetails(String itemName, String category, String color, String description,
                                LocalDate foundDate, String foundPlace, String storagePlace,
                                String imageUrl, String originalUrl) {
    public PoliceItemDetails {
        if (itemName == null) throw new IllegalArgumentException("itemName is required");
        validate(itemName, 200);
        validate(category, 100);
        validate(color, 100);
        validate(foundPlace, 500);
        validate(storagePlace, 200);
        validate(imageUrl, 2048);
        validate(originalUrl, 2048);
    }

    private static void validate(String value, int limit) {
        if (value != null && (value.isBlank() || value.length() > limit)) {
            throw new IllegalArgumentException("Display text is blank or exceeds storage length");
        }
    }
}
