package com.dasifind.backend.domain.searchcard.analysis.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record SearchCardAnalysisRequest(
        @NotBlank @Size(max = 50) String category,
        @NotBlank @Size(max = 100) String itemName,
        @NotNull @Size(max = 10) List<@NotBlank @Size(max = 50) String> color,
        @Size(max = 100) String brand,
        @NotBlank @Size(max = 2000) String featureDescription,
        @NotNull @Size(max = 5) List<@NotNull @Positive Long> imageIds,
        @NotNull @PastOrPresent LocalDate lostDate,
        LocalTime lostStartTime,
        LocalTime lostEndTime,
        @NotNull @Valid LostLocationRequest lostLocation
) {
}
