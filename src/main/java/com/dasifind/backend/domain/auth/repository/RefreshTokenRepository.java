package com.dasifind.backend.domain.auth.repository;

import java.time.Duration;

public interface RefreshTokenRepository {

    void save(String tokenHash, Long userId, Duration ttl);

    void delete(String tokenHash);
}
