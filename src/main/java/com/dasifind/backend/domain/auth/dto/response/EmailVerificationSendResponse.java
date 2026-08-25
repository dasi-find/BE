package com.dasifind.backend.domain.auth.dto.response;

public record EmailVerificationSendResponse(
        String verificationId,
        long expiresInSeconds
) {
}
