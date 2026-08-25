package com.dasifind.backend.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record EmailVerificationConfirmRequest(
        @NotBlank
        @Pattern(regexp = "\\d{6}")
        String verificationCode
) {
}
