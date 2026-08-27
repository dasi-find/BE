package com.dasifind.backend.domain.auth.dto.response;

public record EmailVerificationConfirmResponse(
        String verificationToken,
        String verifiedEmail
) {
}
