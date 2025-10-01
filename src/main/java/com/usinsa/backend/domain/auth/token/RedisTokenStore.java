package com.usinsa.backend.domain.auth.token;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RedisTokenStore implements TokenStore {

    private final StringRedisTemplate redis;

    private static final String REFRESH_PREFIX = "refresh:";
    private static final String BLACKLIST_PREFIX = "bl:";

    @Override
    public void storeRefresh(String userKey, String refreshToken, Duration ttl) {
        redis.opsForValue().set(REFRESH_PREFIX + userKey, refreshToken, ttl);
    }

    @Override
    public Optional<String> getRefresh(String userKey) {
        return Optional.ofNullable(redis.opsForValue().get(REFRESH_PREFIX + userKey));
    }

    @Override
    public void rotateRefresh(String userKey, String newToken, Duration ttl) {
        storeRefresh(userKey, newToken, ttl);
    }

    @Override
    public void blacklistAccess(String accessToken, Duration ttl) {
        redis.opsForValue().set(BLACKLIST_PREFIX + accessToken, "1", ttl);
    }

    @Override
    public boolean isBlacklisted(String accessToken) {
        Boolean hasKey = redis.hasKey(BLACKLIST_PREFIX + accessToken);
        return Boolean.TRUE.equals(hasKey);
    }
}

