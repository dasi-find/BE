package com.dasifind.backend.domain.auth.service;

import com.dasifind.backend.domain.auth.config.EmailVerificationProperties;
import com.dasifind.backend.domain.auth.dto.response.EmailVerificationConfirmResponse;
import com.dasifind.backend.domain.auth.dto.response.EmailVerificationSendResponse;
import com.dasifind.backend.domain.auth.mail.VerificationEmailSender;
import com.dasifind.backend.domain.auth.model.EmailVerificationState;
import com.dasifind.backend.domain.auth.repository.EmailVerificationRepository;
import com.dasifind.backend.domain.user.repository.UserRepository;
import com.dasifind.backend.global.error.BusinessException;
import com.dasifind.backend.global.error.ErrorCode;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

@Service
public class EmailVerificationService {

    private static final int CODE_BOUND = 1_000_000;

    private final UserRepository userRepository;
    private final EmailVerificationRepository verificationRepository;
    private final VerificationEmailSender emailSender;
    private final EmailVerificationProperties properties;
    private final SecureRandom secureRandom;

    public EmailVerificationService(
            UserRepository userRepository,
            EmailVerificationRepository verificationRepository,
            VerificationEmailSender emailSender,
            EmailVerificationProperties properties,
            SecureRandom secureRandom
    ) {
        this.userRepository = userRepository;
        this.verificationRepository = verificationRepository;
        this.emailSender = emailSender;
        this.properties = properties;
        this.secureRandom = secureRandom;
    }

    public EmailVerificationSendResponse send(String rawEmail) {
        String email = normalizeEmail(rawEmail);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        String emailKey = hash(email);
        if (!verificationRepository.acquireSendCooldown(emailKey, properties.resendCooldown())) {
            throw new BusinessException(ErrorCode.RATE_LIMIT_EXCEEDED);
        }

        String verificationId = randomId("ev_");
        String verificationCode = "%06d".formatted(secureRandom.nextInt(CODE_BOUND));
        EmailVerificationState state = new EmailVerificationState(
                email,
                hash(verificationId + ":" + verificationCode),
                0
        );

        verificationRepository.saveRequest(verificationId, state, properties.codeTtl());
        try {
            emailSender.sendVerificationCode(email, verificationCode, properties.codeTtl().toMinutes());
        } catch (RuntimeException exception) {
            verificationRepository.deleteRequest(verificationId);
            verificationRepository.releaseSendCooldown(emailKey);
            throw exception;
        }

        return new EmailVerificationSendResponse(verificationId, properties.codeTtl().toSeconds());
    }

    public EmailVerificationConfirmResponse confirm(String verificationId, String verificationCode) {
        if (verificationRepository.isConfirmed(verificationId)) {
            throw new BusinessException(ErrorCode.DUPLICATE_REQUEST);
        }

        EmailVerificationState state = verificationRepository.findRequest(verificationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EMAIL_VERIFICATION_EXPIRED));

        if (state.attempts() >= properties.maxAttempts()) {
            throw new BusinessException(ErrorCode.RATE_LIMIT_EXCEEDED);
        }

        String requestedHash = hash(verificationId + ":" + verificationCode);
        if (!MessageDigest.isEqual(
                state.codeHash().getBytes(StandardCharsets.UTF_8),
                requestedHash.getBytes(StandardCharsets.UTF_8)
        )) {
            long attempts = verificationRepository.incrementAttempts(verificationId);
            ErrorCode errorCode = attempts >= properties.maxAttempts()
                    ? ErrorCode.RATE_LIMIT_EXCEEDED
                    : ErrorCode.INVALID_VERIFICATION_CODE;
            throw new BusinessException(errorCode);
        }

        if (!verificationRepository.markConfirmed(verificationId, properties.tokenTtl())) {
            throw new BusinessException(ErrorCode.DUPLICATE_REQUEST);
        }

        verificationRepository.deleteRequest(verificationId);
        String verificationToken = randomId("evt_");
        verificationRepository.saveToken(verificationToken, state.email(), properties.tokenTtl());
        return new EmailVerificationConfirmResponse(verificationToken, state.email());
    }

    public void consumeVerificationToken(String verificationToken, String rawEmail) {
        if (!verificationRepository.consumeToken(verificationToken, normalizeEmail(rawEmail))) {
            throw new BusinessException(ErrorCode.EMAIL_VERIFICATION_EXPIRED);
        }
    }

    public void validateVerificationToken(String verificationToken, String rawEmail) {
        if (!verificationRepository.matchesToken(verificationToken, normalizeEmail(rawEmail))) {
            throw new BusinessException(ErrorCode.EMAIL_VERIFICATION_EXPIRED);
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String randomId(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "");
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
