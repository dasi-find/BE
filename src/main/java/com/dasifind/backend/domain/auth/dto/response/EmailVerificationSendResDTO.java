package com.dasifind.backend.domain.auth.dto.response;

public record EmailVerificationSendResDTO(
        String verificationId,
        long expiresInSeconds
) {
}
