package com.usinsa.backend.domain.cart.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;

/**
 * 비회원 장바구니 guestId 관리 (Redis)
 *
 * Key   : guest:cart:{guestId}
 * Value : Cart.id (DB PK) 집합 (Set)
 * TTL   : 7일 — 마지막 항목 추가 시점 기준으로 갱신
 */
@Repository
@RequiredArgsConstructor
public class GuestCartRedisRepository {

    private static final String PREFIX = "guest:cart:";
    private static final Duration TTL = Duration.ofDays(7);

    private final StringRedisTemplate redisTemplate;

    private String key(String guestId) {
        return PREFIX + guestId;
    }

    /** 장바구니 항목 ID 추가 + TTL 갱신 */
    public void addCartId(String guestId, Long cartId) {
        String key = key(guestId);
        redisTemplate.opsForSet().add(key, cartId.toString());
        redisTemplate.expire(key, TTL);
    }

    /** 장바구니 항목 ID 목록 조회 */
    public Set<String> getCartIds(String guestId) {
        Set<String> members = redisTemplate.opsForSet().members(key(guestId));
        return members != null ? members : Collections.emptySet();
    }

    /** 특정 항목 ID 제거 */
    public void removeCartId(String guestId, Long cartId) {
        redisTemplate.opsForSet().remove(key(guestId), cartId.toString());
    }

    /** 비회원 장바구니 키 전체 삭제 */
    public void deleteAll(String guestId) {
        redisTemplate.delete(key(guestId));
    }

    /** TTL 갱신 (조회 시에도 연장하고 싶을 때 호출) */
    public void refreshTtl(String guestId) {
        redisTemplate.expire(key(guestId), TTL);
    }
}
