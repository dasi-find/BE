package com.dasifind.backend.domain.auth.repository;

import java.time.Duration;
import java.util.Optional;

public interface RefreshTokenRepository {

    void save(String tokenHash, Long userId, Duration ttl);

    Optional<Long> findUserId(String tokenHash);

    boolean rotate(String currentTokenHash, String newTokenHash, Duration ttl);

    boolean revoke(String tokenHash, Long userId);

    void delete(String tokenHash);
}
