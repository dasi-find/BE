package com.dasifind.backend.domain.searchcard.dto.request;

import com.dasifind.backend.domain.searchcard.model.SearchCardCloseReason;
import com.dasifind.backend.domain.searchcard.model.SearchCardStatus;
import jakarta.validation.constraints.NotNull;

public record SearchCardCloseRequest(
        @NotNull SearchCardStatus status,
        @NotNull SearchCardCloseReason reason
) {
}
