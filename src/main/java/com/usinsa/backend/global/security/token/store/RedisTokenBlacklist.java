package com.usinsa.backend.global.security.token.store;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class RedisTokenBlacklist implements TokenBlacklist {

    private final StringRedisTemplate redis;

    private String key(String jti) { return "auth:blacklist:" + jti; }

    @Override
    public void blacklist(String jti, Instant expiresAt) {
        long ttl = Math.max(1, expiresAt.getEpochSecond() - Instant.now().getEpochSecond());
        redis.opsForValue().set(key(jti), "1", Duration.ofSeconds(ttl));
    }

    @Override
    public boolean isBlacklisted(String jti) {
        Boolean exists = redis.hasKey(key(jti));
        return exists != null && exists;
    }
}
