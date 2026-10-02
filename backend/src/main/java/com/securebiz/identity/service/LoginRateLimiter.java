package com.securebiz.identity.service;

import com.securebiz.identity.api.AuthException;
import com.securebiz.identity.config.AuthProperties;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
public class LoginRateLimiter {
    private final StringRedisTemplate redis;
    private final AuthProperties properties;

    public LoginRateLimiter(StringRedisTemplate redis, AuthProperties properties) {
        this.redis = redis;
        this.properties = properties;
    }

    public void check(String identifier, String ipAddress) {
        String key = "auth:login:" + sha256(identifier + "|" + ipAddress);
        try {
            Long count = redis.opsForValue().increment(key);
            if (count != null && count == 1) {
                redis.expire(key, properties.loginRateWindow());
            }
            if (count != null && count > properties.loginRateLimit()) {
                throw new AuthException(HttpStatus.TOO_MANY_REQUESTS, "LOGIN_RATE_LIMITED",
                        "Too many login attempts. Try again later.");
            }
        } catch (DataAccessException exception) {
            throw new AuthException(HttpStatus.SERVICE_UNAVAILABLE, "AUTH_RATE_LIMIT_UNAVAILABLE",
                    "Authentication is temporarily unavailable");
        }
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }
}
