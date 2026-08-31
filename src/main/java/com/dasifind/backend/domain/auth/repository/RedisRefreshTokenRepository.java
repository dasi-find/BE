package com.dasifind.backend.domain.auth.repository;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Repository
public class RedisRefreshTokenRepository implements RefreshTokenRepository {

    private static final String KEY_PREFIX = "auth:refresh-token:";
    private static final DefaultRedisScript<Long> ROTATE_SCRIPT = new DefaultRedisScript<>("""
            local userId = redis.call('GET', KEYS[1])
            if not userId then
                return 0
            end
            redis.call('SET', KEYS[2], userId, 'PX', ARGV[1])
            redis.call('DEL', KEYS[1])
            return 1
            """, Long.class);

    private final StringRedisTemplate redisTemplate;

    public RedisRefreshTokenRepository(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void save(String tokenHash, Long userId, Duration ttl) {
        redisTemplate.opsForValue().set(KEY_PREFIX + tokenHash, userId.toString(), ttl);
    }

    @Override
    public Optional<Long> findUserId(String tokenHash) {
        String userId = redisTemplate.opsForValue().get(KEY_PREFIX + tokenHash);
        return Optional.ofNullable(userId).map(Long::valueOf);
    }

    @Override
    public boolean rotate(String currentTokenHash, String newTokenHash, Duration ttl) {
        Long result = redisTemplate.execute(
                ROTATE_SCRIPT,
                List.of(KEY_PREFIX + currentTokenHash, KEY_PREFIX + newTokenHash),
                Long.toString(ttl.toMillis())
        );
        return Objects.equals(result, 1L);
    }

    @Override
    public void delete(String tokenHash) {
        redisTemplate.delete(KEY_PREFIX + tokenHash);
    }
}
