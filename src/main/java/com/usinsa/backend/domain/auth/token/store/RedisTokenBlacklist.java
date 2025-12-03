package com.usinsa.backend.domain.auth.token.store;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Redis를 이용한 Token Blacklist 구현체
 * 로그아웃된 Access Token을 블랙리스트에 등록
 */
@Component
@RequiredArgsConstructor
public class RedisTokenBlacklist implements TokenBlacklist {

    private final StringRedisTemplate redis;

    /**
     * Redis key 생성
     * 형식: auth:blacklist:{jti}
     */
    private String key(String jti) {
        return "auth:blacklist:" + jti;
    }

    @Override
    public void blacklist(String jti, Instant expiresAt) {
        // TTL 설정 (토큰 만료 시간까지만 블랙리스트에 유지)
        long ttl = Math.max(1, expiresAt.getEpochSecond() - Instant.now().getEpochSecond());
        redis.opsForValue().set(key(jti), "1", Duration.ofSeconds(ttl));
    }

    @Override
    public boolean isBlacklisted(String jti) {
        Boolean exists = redis.hasKey(key(jti));
        return exists != null && exists;
    }
}
