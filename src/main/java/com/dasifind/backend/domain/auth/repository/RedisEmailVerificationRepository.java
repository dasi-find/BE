package com.dasifind.backend.domain.auth.repository;

import com.dasifind.backend.domain.auth.model.EmailVerificationState;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class RedisEmailVerificationRepository implements EmailVerificationRepository {

    private static final String REQUEST_PREFIX = "auth:email-verification:request:";
    private static final String COOLDOWN_PREFIX = "auth:email-verification:cooldown:";
    private static final String CONFIRMED_PREFIX = "auth:email-verification:confirmed:";
    private static final String TOKEN_PREFIX = "auth:email-verification:token:";
    private static final DefaultRedisScript<Long> CONSUME_TOKEN_SCRIPT = new DefaultRedisScript<>("""
            local value = redis.call('GET', KEYS[1])
            if not value or value ~= ARGV[1] then
                return 0
            end
            redis.call('DEL', KEYS[1])
            return 1
            """, Long.class);

    private final StringRedisTemplate redisTemplate;

    public RedisEmailVerificationRepository(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean acquireSendCooldown(String emailKey, Duration ttl) {
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(COOLDOWN_PREFIX + emailKey, "1", ttl);
        return Boolean.TRUE.equals(acquired);
    }

    @Override
    public void releaseSendCooldown(String emailKey) {
        redisTemplate.delete(COOLDOWN_PREFIX + emailKey);
    }

    @Override
    public void saveRequest(String verificationId, EmailVerificationState state, Duration ttl) {
        String key = REQUEST_PREFIX + verificationId;
        redisTemplate.opsForHash().putAll(key, Map.of(
                "email", state.email(),
                "codeHash", state.codeHash(),
                "attempts", Integer.toString(state.attempts())
        ));
        redisTemplate.expire(key, ttl);
    }

    @Override
    public Optional<EmailVerificationState> findRequest(String verificationId) {
        Map<Object, Object> values = redisTemplate.opsForHash().entries(REQUEST_PREFIX + verificationId);
        if (values.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new EmailVerificationState(
                values.get("email").toString(),
                values.get("codeHash").toString(),
                Integer.parseInt(values.get("attempts").toString())
        ));
    }

    @Override
    public long incrementAttempts(String verificationId) {
        Long attempts = redisTemplate.opsForHash()
                .increment(REQUEST_PREFIX + verificationId, "attempts", 1);
        return attempts == null ? 0 : attempts;
    }

    @Override
    public void deleteRequest(String verificationId) {
        redisTemplate.delete(REQUEST_PREFIX + verificationId);
    }

    @Override
    public boolean isConfirmed(String verificationId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(CONFIRMED_PREFIX + verificationId));
    }

    @Override
    public boolean markConfirmed(String verificationId, Duration ttl) {
        Boolean marked = redisTemplate.opsForValue()
                .setIfAbsent(CONFIRMED_PREFIX + verificationId, "1", ttl);
        return Boolean.TRUE.equals(marked);
    }

    @Override
    public void saveToken(String verificationToken, String email, Duration ttl) {
        redisTemplate.opsForValue().set(TOKEN_PREFIX + verificationToken, email, ttl);
    }

    @Override
    public boolean matchesToken(String verificationToken, String email) {
        String savedEmail = redisTemplate.opsForValue().get(TOKEN_PREFIX + verificationToken);
        return email.equals(savedEmail);
    }

    @Override
    public boolean consumeToken(String verificationToken, String email) {
        Long consumed = redisTemplate.execute(
                CONSUME_TOKEN_SCRIPT,
                List.of(TOKEN_PREFIX + verificationToken),
                email
        );
        return consumed != null && consumed == 1L;
    }
}
