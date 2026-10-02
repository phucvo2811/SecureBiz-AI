package com.securebiz.identity.service;

import com.securebiz.identity.api.AuthException;
import com.securebiz.identity.config.AuthProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class LoginRateLimiterTest {
    private StringRedisTemplate redis;
    private ValueOperations<String, String> values;
    private LoginRateLimiter limiter;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redis = mock(StringRedisTemplate.class);
        values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        limiter = new LoginRateLimiter(redis, new AuthProperties(
                "securebiz", "01234567890123456789012345678901",
                Duration.ofMinutes(15), Duration.ofDays(7), 5, Duration.ofMinutes(15),
                3, Duration.ofMinutes(1), false));
    }

    @Test
    void firstAttemptStartsWindowWithoutExposingIdentifierInRedisKey() {
        when(values.increment(anyString())).thenReturn(1L);

        limiter.check("owner@example.com", "127.0.0.1");

        var key = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(values).increment(key.capture());
        assertThat(key.getValue()).startsWith("auth:login:")
                .doesNotContain("owner@example.com").doesNotContain("127.0.0.1");
        verify(redis).expire(key.getValue(), Duration.ofMinutes(1));
    }

    @Test
    void attemptBeyondConfiguredLimitIsRejected() {
        when(values.increment(anyString())).thenReturn(4L);

        assertThatThrownBy(() -> limiter.check("owner@example.com", "127.0.0.1"))
                .isInstanceOfSatisfying(AuthException.class, error -> {
                    assertThat(error.code()).isEqualTo("LOGIN_RATE_LIMITED");
                    assertThat(error.status().value()).isEqualTo(429);
                });
        verify(redis, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    void redisFailureFailsClosed() {
        when(values.increment(anyString())).thenThrow(new DataAccessResourceFailureException("offline"));

        assertThatThrownBy(() -> limiter.check("owner@example.com", "127.0.0.1"))
                .isInstanceOfSatisfying(AuthException.class, error -> {
                    assertThat(error.code()).isEqualTo("AUTH_RATE_LIMIT_UNAVAILABLE");
                    assertThat(error.status().value()).isEqualTo(503);
                });
    }
}
