package com.dasifind.backend.domain.auth.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

public record SignupAgreementsRequest(
        @NotNull @AssertTrue Boolean terms,
        @NotNull @AssertTrue Boolean privacy,
        @NotNull Boolean emailNotification
) {
}
