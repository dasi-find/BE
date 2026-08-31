package com.dasifind.backend.domain.auth.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisRefreshTokenRepositoryTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisRefreshTokenRepository repository;

    @BeforeEach
    void setUp() {
        repository = new RedisRefreshTokenRepository(redisTemplate);
    }

    @Test
    void 토큰_해시로_사용자_아이디를_조회한다() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("auth:refresh-token:token-hash")).thenReturn("7");

        Optional<Long> userId = repository.findUserId("token-hash");

        assertThat(userId).contains(7L);
    }

    @Test
    @SuppressWarnings("unchecked")
    void 기존_토큰을_새_토큰으로_원자적으로_교체한다() {
        when(redisTemplate.execute(
                any(RedisScript.class),
                eq(List.of("auth:refresh-token:current-hash", "auth:refresh-token:new-hash")),
                eq("1209600000")
        )).thenReturn(1L);

        boolean rotated = repository.rotate(
                "current-hash",
                "new-hash",
                Duration.ofDays(14)
        );

        assertThat(rotated).isTrue();
        ArgumentCaptor<RedisScript<Long>> scriptCaptor = ArgumentCaptor.forClass(RedisScript.class);
        verify(redisTemplate).execute(
                scriptCaptor.capture(),
                eq(List.of("auth:refresh-token:current-hash", "auth:refresh-token:new-hash")),
                eq("1209600000")
        );
        assertThat(scriptCaptor.getValue().getScriptAsString())
                .contains("redis.call('GET', KEYS[1])")
                .contains("redis.call('SET', KEYS[2]")
                .contains("redis.call('DEL', KEYS[1])");
    }

    @Test
    @SuppressWarnings("unchecked")
    void 기존_토큰이_이미_없으면_교체에_실패한다() {
        when(redisTemplate.execute(any(RedisScript.class), any(List.class), any()))
                .thenReturn(0L);

        boolean rotated = repository.rotate("missing-hash", "new-hash", Duration.ofDays(14));

        assertThat(rotated).isFalse();
    }
}
