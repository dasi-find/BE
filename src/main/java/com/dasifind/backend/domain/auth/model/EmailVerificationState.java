package com.dasifind.backend.domain.auth.model;

public record EmailVerificationState(
        String email,
        String codeHash,
        int attempts
) {
}
