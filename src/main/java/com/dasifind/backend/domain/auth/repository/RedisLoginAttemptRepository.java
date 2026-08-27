package com.dasifind.backend.domain.auth.repository;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;

@Repository
public class RedisLoginAttemptRepository implements LoginAttemptRepository {

    private static final String KEY_PREFIX = "auth:login-attempt:";
    private static final DefaultRedisScript<Long> INCREMENT_SCRIPT = new DefaultRedisScript<>("""
            local attempts = redis.call('INCR', KEYS[1])
            redis.call('PEXPIRE', KEYS[1], ARGV[1])
            return attempts
            """, Long.class);

    private final StringRedisTemplate redisTemplate;

    public RedisLoginAttemptRepository(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public long getAttempts(String emailKey) {
        String value = redisTemplate.opsForValue().get(KEY_PREFIX + emailKey);
        return value == null ? 0 : Long.parseLong(value);
    }

    @Override
    public long incrementAttempts(String emailKey, Duration ttl) {
        Long attempts = redisTemplate.execute(
                INCREMENT_SCRIPT,
                List.of(KEY_PREFIX + emailKey),
                Long.toString(ttl.toMillis())
        );
        return attempts == null ? 0 : attempts;
    }

    @Override
    public void clear(String emailKey) {
        redisTemplate.delete(KEY_PREFIX + emailKey);
    }
}
