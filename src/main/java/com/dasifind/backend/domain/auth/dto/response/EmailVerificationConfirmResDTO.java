package com.dasifind.backend.domain.auth.dto.response;

public record EmailVerificationConfirmResDTO(
        String verificationToken,
        String verifiedEmail
) {
}
