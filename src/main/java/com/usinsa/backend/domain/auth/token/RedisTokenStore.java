package com.usinsa.backend.domain.auth.token;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RedisTokenStore implements TokenStore {
    private final StringRedisTemplate redis;
    private static final String REFRESH = "refresh:";
    private static final String BL = "bl:";

    public void storeRefresh(String userKey, String token, Duration ttl) {
        redis.opsForValue().set(REFRESH + userKey, token, ttl);
    }
    public Optional<String> getRefresh(String userKey) {
        return Optional.ofNullable(redis.opsForValue().get(REFRESH + userKey));
    }
    public void rotateRefresh(String userKey, String newToken, Duration ttl) {
        storeRefresh(userKey, newToken, ttl);
    }
    public void blacklistAccess(String accessToken, Duration ttl) {
        redis.opsForValue().set(BL + accessToken, "1", ttl);
    }
    public boolean isBlacklisted(String accessToken) {
        return Boolean.TRUE.equals(redis.hasKey(BL + accessToken));
    }
}

