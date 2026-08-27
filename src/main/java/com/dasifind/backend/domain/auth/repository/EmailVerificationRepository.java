package com.dasifind.backend.domain.auth.repository;

import com.dasifind.backend.domain.auth.model.EmailVerificationState;

import java.time.Duration;
import java.util.Optional;

public interface EmailVerificationRepository {

    boolean acquireSendCooldown(String emailKey, Duration ttl);

    void releaseSendCooldown(String emailKey);

    void saveRequest(String verificationId, EmailVerificationState state, Duration ttl);

    Optional<EmailVerificationState> findRequest(String verificationId);

    long incrementAttempts(String verificationId);

    void deleteRequest(String verificationId);

    boolean isConfirmed(String verificationId);

    boolean markConfirmed(String verificationId, Duration ttl);

    void saveToken(String verificationToken, String email, Duration ttl);

    boolean matchesToken(String verificationToken, String email);

    boolean consumeToken(String verificationToken, String email);
}
