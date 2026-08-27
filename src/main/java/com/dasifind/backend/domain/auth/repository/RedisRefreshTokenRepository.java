package com.dasifind.backend.domain.auth.repository;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;

@Repository
public class RedisRefreshTokenRepository implements RefreshTokenRepository {

    private static final String KEY_PREFIX = "auth:refresh-token:";

    private final StringRedisTemplate redisTemplate;

    public RedisRefreshTokenRepository(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void save(String tokenHash, Long userId, Duration ttl) {
        redisTemplate.opsForValue().set(KEY_PREFIX + tokenHash, userId.toString(), ttl);
    }

    @Override
    public void delete(String tokenHash) {
        redisTemplate.delete(KEY_PREFIX + tokenHash);
    }
}
