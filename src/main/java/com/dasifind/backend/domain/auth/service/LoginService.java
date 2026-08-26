package com.dasifind.backend.domain.auth.service;

import com.dasifind.backend.domain.auth.dto.request.LoginRequest;
import com.dasifind.backend.domain.auth.dto.response.AuthUserResponse;
import com.dasifind.backend.domain.auth.dto.response.LoginResponse;
import com.dasifind.backend.domain.auth.model.IssuedTokens;
import com.dasifind.backend.domain.auth.model.LoginResult;
import com.dasifind.backend.domain.user.entity.User;
import com.dasifind.backend.domain.user.repository.UserRepository;
import com.dasifind.backend.global.error.BusinessException;
import com.dasifind.backend.global.error.ErrorCode;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;

@Service
public class LoginService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;
    private final AuthTokenService authTokenService;
    private final String dummyPasswordHash;

    public LoginService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            LoginAttemptService loginAttemptService,
            AuthTokenService authTokenService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.loginAttemptService = loginAttemptService;
        this.authTokenService = authTokenService;
        this.dummyPasswordHash = passwordEncoder.encode("dummy-password-for-timing-protection");
    }

    public LoginResult login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        loginAttemptService.ensureAllowed(email);

        Optional<User> optionalUser = userRepository.findByEmailIgnoreCase(email);
        String encodedPassword = optionalUser.map(User::getPassword).orElse(dummyPasswordHash);
        boolean passwordMatches = passwordEncoder.matches(request.password(), encodedPassword);
        if (optionalUser.isEmpty() || !passwordMatches) {
            loginAttemptService.recordFailure(email);
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        User user = optionalUser.get();
        loginAttemptService.clear(email);
        IssuedTokens tokens = authTokenService.issue(user.getId());
        LoginResponse response = new LoginResponse(
                AuthUserResponse.from(user),
                tokens.accessToken(),
                tokens.accessTokenExpiresInSeconds()
        );
        return new LoginResult(response, tokens.refreshToken());
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
