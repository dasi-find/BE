package com.dasifind.backend.domain.auth.service;

import com.dasifind.backend.domain.auth.config.LoginAttemptProperties;
import com.dasifind.backend.domain.auth.repository.LoginAttemptRepository;
import com.dasifind.backend.global.error.BusinessException;
import com.dasifind.backend.global.error.ErrorCode;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
public class LoginAttemptService {

    private final LoginAttemptRepository loginAttemptRepository;
    private final LoginAttemptProperties properties;

    public LoginAttemptService(
            LoginAttemptRepository loginAttemptRepository,
            LoginAttemptProperties properties
    ) {
        this.loginAttemptRepository = loginAttemptRepository;
        this.properties = properties;
    }

    public void ensureAllowed(String email) {
        if (loginAttemptRepository.getAttempts(emailKey(email)) >= properties.maxAttempts()) {
            throw new BusinessException(ErrorCode.RATE_LIMIT_EXCEEDED);
        }
    }

    public void recordFailure(String email) {
        loginAttemptRepository.incrementAttempts(emailKey(email), properties.blockDuration());
    }

    public void clear(String email) {
        loginAttemptRepository.clear(emailKey(email));
    }

    private String emailKey(String email) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(email.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
