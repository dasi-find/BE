package com.dasifind.backend.domain.auth.repository;

import java.time.Duration;

public interface LoginAttemptRepository {

    long getAttempts(String emailKey);

    long incrementAttempts(String emailKey, Duration ttl);

    void clear(String emailKey);
}
