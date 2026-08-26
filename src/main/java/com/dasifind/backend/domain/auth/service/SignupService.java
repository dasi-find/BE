package com.dasifind.backend.domain.auth.service;

import com.dasifind.backend.domain.auth.dto.request.SignupRequest;
import com.dasifind.backend.domain.auth.dto.response.AuthUserResponse;
import com.dasifind.backend.domain.auth.dto.response.SignupResponse;
import com.dasifind.backend.domain.auth.model.IssuedTokens;
import com.dasifind.backend.domain.auth.model.SignupResult;
import com.dasifind.backend.domain.user.entity.User;
import com.dasifind.backend.domain.user.repository.UserRepository;
import com.dasifind.backend.global.error.BusinessException;
import com.dasifind.backend.global.error.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class SignupService {

    private static final Logger log = LoggerFactory.getLogger(SignupService.class);

    private final UserRepository userRepository;
    private final EmailVerificationService emailVerificationService;
    private final PasswordEncoder passwordEncoder;
    private final AuthTokenService authTokenService;

    public SignupService(
            UserRepository userRepository,
            EmailVerificationService emailVerificationService,
            PasswordEncoder passwordEncoder,
            AuthTokenService authTokenService
    ) {
        this.userRepository = userRepository;
        this.emailVerificationService = emailVerificationService;
        this.passwordEncoder = passwordEncoder;
        this.authTokenService = authTokenService;
    }

    @Transactional
    public SignupResult signup(SignupRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        emailVerificationService.validateVerificationToken(request.verificationToken(), email);

        User user = User.create(
                email,
                passwordEncoder.encode(request.password()),
                request.name().trim(),
                request.agreements().emailNotification()
        );
        User savedUser;
        try {
            savedUser = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        IssuedTokens tokens = authTokenService.issue(savedUser.getId());
        try {
            emailVerificationService.consumeVerificationToken(request.verificationToken(), email);
        } catch (RuntimeException exception) {
            revokeRefreshTokenAfterFailure(tokens.refreshToken(), exception);
            throw exception;
        }
        SignupResponse response = new SignupResponse(
                AuthUserResponse.from(savedUser),
                tokens.accessToken(),
                tokens.accessTokenExpiresInSeconds()
        );
        return new SignupResult(response, tokens.refreshToken());
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private void revokeRefreshTokenAfterFailure(String refreshToken, RuntimeException originalException) {
        try {
            authTokenService.revokeRefreshToken(refreshToken);
        } catch (RuntimeException revokeException) {
            originalException.addSuppressed(revokeException);
            log.error("Failed to revoke refresh token after signup failure", revokeException);
        }
    }
}
