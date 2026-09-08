package com.dasifind.backend.domain.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailVerificationSendReqDTO(
        @NotBlank
        @Email
        String email
) {
}
