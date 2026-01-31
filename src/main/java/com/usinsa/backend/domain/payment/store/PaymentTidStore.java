package com.usinsa.backend.domain.payment.store;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * Redis를 이용한 결제 TID 저장소
 * 결제 준비 시 생성된 TID를 주문 ID와 매핑하여 저장
 */
@Component
@RequiredArgsConstructor
public class PaymentTidStore {

    private final StringRedisTemplate redis;
    
    private static final String KEY_PREFIX = "payment:tid:";
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(30); // 30분

    /**
     * Redis key 생성
     * 형식: payment:tid:{orderId}
     */
    private String key(Long orderId) {
        return KEY_PREFIX + orderId;
    }

    /**
     * TID 저장
     * @param orderId 주문 ID
     * @param tid 카카오페이 결제 고유번호
     */
    public void save(Long orderId, String tid) {
        redis.opsForValue().set(key(orderId), tid, DEFAULT_TTL);
    }

    /**
     * TID 조회
     * @param orderId 주문 ID
     * @return TID
     */
    public Optional<String> find(Long orderId) {
        String tid = redis.opsForValue().get(key(orderId));
        return Optional.ofNullable(tid);
    }

    /**
     * TID 삭제
     * @param orderId 주문 ID
     */
    public void delete(Long orderId) {
        redis.delete(key(orderId));
    }

    /**
     * TID가 존재하는지 확인
     * @param orderId 주문 ID
     * @return 존재 여부
     */
    public boolean exists(Long orderId) {
        return Boolean.TRUE.equals(redis.hasKey(key(orderId)));
    }
}
