package com.usinsa.backend.domain.auth.token.store;

import com.usinsa.backend.domain.auth.token.TokenMeta;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Redis를 이용한 Refresh Token 저장소 구현체
 */
@Component
@RequiredArgsConstructor
public class RedisRefreshTokenStore implements RefreshTokenStore {

    private final StringRedisTemplate redis;

    /**
     * Redis key 생성
     * 형식: auth:refresh:{memberId}:{deviceId}
     */
    private String key(Long memberId, String deviceId) {
        return "auth:refresh:" + memberId + ":" + deviceId;
    }

    @Override
    public void save(TokenMeta meta) {
        String k = key(meta.getMemberId(), meta.getDeviceId());
        
        Map<String, String> map = new HashMap<>();
        map.put("jti", meta.getJti());
        map.put("memberId", String.valueOf(meta.getMemberId()));
        map.put("email", meta.getEmail() == null ? "" : meta.getEmail());
        map.put("roles", meta.getRoles() == null ? "" : String.join(",", meta.getRoles()));
        map.put("deviceId", meta.getDeviceId());
        map.put("expEpoch", String.valueOf(meta.getExpiresAt().getEpochSecond()));

        redis.opsForHash().putAll(k, map);

        // TTL 설정 (만료 시간까지의 남은 시간)
        long ttl = Math.max(1, meta.getExpiresAt().getEpochSecond() - Instant.now().getEpochSecond());
        redis.expire(k, Duration.ofSeconds(ttl));
    }

    @Override
    public Optional<TokenMeta> find(Long memberId, String deviceId) {
        String k = key(memberId, deviceId);
        Map<Object, Object> m = redis.opsForHash().entries(k);
        
        if (m == null || m.isEmpty()) {
            return Optional.empty();
        }

        // 만료 시간 확인
        long expEpoch = Long.parseLong(String.valueOf(m.getOrDefault("expEpoch", "0")));
        Instant exp = Instant.ofEpochSecond(expEpoch);
        
        if (exp.isBefore(Instant.now())) {
            redis.delete(k);
            return Optional.empty();
        }

        // roles 파싱
        String rolesStr = String.valueOf(m.getOrDefault("roles", ""));
        List<String> roles = rolesStr.isBlank()
                ? Collections.emptyList()
                : Arrays.asList(rolesStr.split(","));

        TokenMeta meta = TokenMeta.builder()
                .jti(String.valueOf(m.get("jti")))
                .memberId(Long.parseLong(String.valueOf(m.get("memberId"))))
                .email(String.valueOf(m.get("email")))
                .roles(roles)
                .deviceId(String.valueOf(m.get("deviceId")))
                .expiresAt(exp)
                .build();
        
        return Optional.of(meta);
    }

    @Override
    public void delete(Long memberId, String deviceId) {
        redis.delete(key(memberId, deviceId));
    }
}
