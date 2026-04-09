package com.usinsa.backend.domain.product.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;

/**
 * ProductLike Redis Cache Service
 * Cache Aside Pattern 구현
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductLikeCacheService {

    private final StringRedisTemplate redis;

    // Redis Key Patterns
    private static final String LIKE_COUNT_KEY = "product:like:count:%d";        // 상품별 좋아요 개수
    private static final String MEMBER_LIKE_KEY = "product:like:member:%d";     // 회원별 좋아요한 상품 Set
    private static final String PRODUCT_LIKERS_KEY = "product:like:likers:%d"; // 상품별 좋아요한 회원 Set
    
    // TTL
    private static final Duration LIKE_COUNT_TTL = Duration.ofHours(24);
    private static final Duration MEMBER_LIKE_TTL = Duration.ofHours(12);
    private static final Duration PRODUCT_LIKERS_TTL = Duration.ofHours(12);

    // ==================== 좋아요 개수 캐시 ====================
    
    /**
     * 좋아요 개수 조회 (캐시)
     */
    public Integer getLikeCount(Long productId) {
        String key = String.format(LIKE_COUNT_KEY, productId);
        String value = redis.opsForValue().get(key);
        
        if (value == null) {
            log.debug("좋아요 개수 캐시 미스: productId={}", productId);
            return null;
        }
        
        log.debug("좋아요 개수 캐시 히트: productId={}, count={}", productId, value);
        return Integer.parseInt(value);
    }

    /**
     * 좋아요 개수 저장 (캐시)
     */
    public void setLikeCount(Long productId, int count) {
        String key = String.format(LIKE_COUNT_KEY, productId);
        redis.opsForValue().set(key, String.valueOf(count), LIKE_COUNT_TTL);
        log.debug("좋아요 개수 캐시 저장: productId={}, count={}", productId, count);
    }

    /**
     * 좋아요 개수 증가
     */
    public Long incrementLikeCount(Long productId) {
        String key = String.format(LIKE_COUNT_KEY, productId);
        Long newCount = redis.opsForValue().increment(key);
        redis.expire(key, LIKE_COUNT_TTL);
        log.debug("좋아요 개수 증가: productId={}, newCount={}", productId, newCount);
        return newCount;
    }

    /**
     * 좋아요 개수 감소
     */
    public Long decrementLikeCount(Long productId) {
        String key = String.format(LIKE_COUNT_KEY, productId);
        Long newCount = redis.opsForValue().decrement(key);
        redis.expire(key, LIKE_COUNT_TTL);
        log.debug("좋아요 개수 감소: productId={}, newCount={}", productId, newCount);
        return newCount;
    }

    /**
     * 좋아요 개수 캐시 삭제
     */
    public void deleteLikeCount(Long productId) {
        String key = String.format(LIKE_COUNT_KEY, productId);
        redis.delete(key);
        log.debug("좋아요 개수 캐시 삭제: productId={}", productId);
    }

    // ==================== 회원-상품 좋아요 관계 캐시 ====================
    
    /**
     * 회원이 특정 상품을 좋아요했는지 확인 (캐시)
     */
    public Boolean isMemberLikedProduct(Long memberId, Long productId) {
        String key = String.format(MEMBER_LIKE_KEY, memberId);
        
        // 캐시에 회원의 좋아요 목록이 있는지 확인
        if (!redis.hasKey(key)) {
            log.debug("회원 좋아요 목록 캐시 미스: memberId={}", memberId);
            return null;
        }
        
        Boolean isMember = redis.opsForSet().isMember(key, String.valueOf(productId));
        log.debug("회원 좋아요 여부 캐시 히트: memberId={}, productId={}, liked={}", 
                  memberId, productId, isMember);
        return isMember;
    }

    /**
     * 회원의 좋아요 목록 저장 (캐시)
     */
    public void setMemberLikedProducts(Long memberId, Set<Long> productIds) {
        String key = String.format(MEMBER_LIKE_KEY, memberId);
        
        // 기존 데이터 삭제
        redis.delete(key);
        
        if (productIds != null && !productIds.isEmpty()) {
            String[] productIdStrings = productIds.stream()
                    .map(String::valueOf)
                    .toArray(String[]::new);
            redis.opsForSet().add(key, productIdStrings);
            redis.expire(key, MEMBER_LIKE_TTL);
        }
        
        log.debug("회원 좋아요 목록 캐시 저장: memberId={}, count={}", memberId, productIds.size());
    }

    /**
     * 회원의 좋아요 목록에 상품 추가
     */
    public void addMemberLike(Long memberId, Long productId) {
        String memberKey = String.format(MEMBER_LIKE_KEY, memberId);
        String productKey = String.format(PRODUCT_LIKERS_KEY, productId);
        
        redis.opsForSet().add(memberKey, String.valueOf(productId));
        redis.expire(memberKey, MEMBER_LIKE_TTL);
        
        redis.opsForSet().add(productKey, String.valueOf(memberId));
        redis.expire(productKey, PRODUCT_LIKERS_TTL);
        
        log.debug("회원 좋아요 추가: memberId={}, productId={}", memberId, productId);
    }

    /**
     * 회원의 좋아요 목록에서 상품 제거
     */
    public void removeMemberLike(Long memberId, Long productId) {
        String memberKey = String.format(MEMBER_LIKE_KEY, memberId);
        String productKey = String.format(PRODUCT_LIKERS_KEY, productId);
        
        redis.opsForSet().remove(memberKey, String.valueOf(productId));
        redis.opsForSet().remove(productKey, String.valueOf(memberId));
        
        log.debug("회원 좋아요 제거: memberId={}, productId={}", memberId, productId);
    }

    /**
     * 회원의 좋아요 목록 캐시 삭제
     */
    public void deleteMemberLikes(Long memberId) {
        String key = String.format(MEMBER_LIKE_KEY, memberId);
        redis.delete(key);
        log.debug("회원 좋아요 목록 캐시 삭제: memberId={}", memberId);
    }

    /**
     * 상품의 좋아요한 회원 목록 캐시 삭제
     */
    public void deleteProductLikers(Long productId) {
        String key = String.format(PRODUCT_LIKERS_KEY, productId);
        redis.delete(key);
        log.debug("상품 좋아요한 회원 목록 캐시 삭제: productId={}", productId);
    }

    // ==================== 캐시 무효화 ====================
    
    /**
     * 특정 상품의 모든 캐시 무효화
     */
    public void invalidateProductCache(Long productId) {
        deleteLikeCount(productId);
        deleteProductLikers(productId);
        log.debug("상품 캐시 전체 무효화: productId={}", productId);
    }

    /**
     * 특정 회원의 모든 캐시 무효화
     */
    public void invalidateMemberCache(Long memberId) {
        deleteMemberLikes(memberId);
        log.debug("회원 캐시 전체 무효화: memberId={}", memberId);
    }
}
