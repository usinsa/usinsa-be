package com.usinsa.backend.domain.product.service;

import com.usinsa.backend.domain.member.entity.Member;
import com.usinsa.backend.domain.member.repository.MemberRepository;
import com.usinsa.backend.domain.product.cache.ProductLikeCacheService;
import com.usinsa.backend.domain.product.dto.ProductLikeDto;
import com.usinsa.backend.domain.product.entity.Product;
import com.usinsa.backend.domain.product.entity.ProductLike;
import com.usinsa.backend.domain.product.repository.ProductLikeRepository;
import com.usinsa.backend.domain.product.repository.ProductRepository;
import com.usinsa.backend.global.exception.CustomException;
import com.usinsa.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * ProductLike Service with Cache Aside Pattern
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductLikeService {

    private final ProductLikeRepository productLikeRepository;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;
    private final ProductLikeCacheService cacheService;

    /**
     * 좋아요 추가 (Cache Aside Pattern)
     */
    @Transactional
    public ProductLikeDto.Response addLike(Long memberId, Long productId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        // 1. 캐시에서 중복 체크 (Cache Aside)
        Boolean cachedLiked = cacheService.isMemberLikedProduct(memberId, productId);
        
        if (cachedLiked == null) {
            // 캐시 미스 - DB에서 조회
            boolean dbLiked = productLikeRepository.existsByMemberIdAndProductId(memberId, productId);
            if (dbLiked) {
                // 캐시 갱신 후 예외 발생
                cacheService.addMemberLike(memberId, productId);
                throw new CustomException(ErrorCode.PRODUCT_ALREADY_LIKED);
            }
        } else if (cachedLiked) {
            // 캐시 히트 - 이미 좋아요한 상태
            throw new CustomException(ErrorCode.PRODUCT_ALREADY_LIKED);
        }

        // 2. DB에 저장
        ProductLike productLike = ProductLike.builder()
                .member(member)
                .product(product)
                .build();

        productLikeRepository.save(productLike);
        log.info("좋아요 추가: memberId={}, productId={}", memberId, productId);

        // 3. 캐시 업데이트
        cacheService.addMemberLike(memberId, productId);
        
        // 4. 좋아요 개수 캐시 업데이트 (증가)
        Long newCount = cacheService.incrementLikeCount(productId);
        int likeCount;
        
        if (newCount == null || newCount == 1L) {
            // 캐시가 없었거나 첫 좋아요인 경우 DB에서 정확한 개수 조회 후 캐시 저장
            likeCount = productLikeRepository.countByProductId(productId);
            cacheService.setLikeCount(productId, likeCount);
        } else {
            likeCount = newCount.intValue();
        }

        // 5. Product 엔티티 업데이트
        product.updateLikeCount(likeCount);

        return ProductLikeDto.Response.builder()
                .productId(productId)
                .liked(true)
                .likeCount(likeCount)
                .build();
    }

    /**
     * 좋아요 취소 (Cache Aside Pattern)
     */
    @Transactional
    public ProductLikeDto.Response removeLike(Long memberId, Long productId) {
        // 1. 캐시에서 좋아요 여부 확인 (Cache Aside)
        Boolean cachedLiked = cacheService.isMemberLikedProduct(memberId, productId);
        
        ProductLike productLike;
        
        if (cachedLiked == null) {
            // 캐시 미스 - DB에서 조회
            productLike = productLikeRepository.findByMemberIdAndProductId(memberId, productId)
                    .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_LIKE_NOT_FOUND));
        } else if (!cachedLiked) {
            // 캐시 히트 - 좋아요하지 않은 상태
            throw new CustomException(ErrorCode.PRODUCT_LIKE_NOT_FOUND);
        } else {
            // 캐시 히트 - 좋아요한 상태, DB에서 삭제 대상 조회
            productLike = productLikeRepository.findByMemberIdAndProductId(memberId, productId)
                    .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_LIKE_NOT_FOUND));
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        // 2. DB에서 삭제
        productLikeRepository.delete(productLike);
        log.info("좋아요 취소: memberId={}, productId={}", memberId, productId);

        // 3. 캐시 업데이트
        cacheService.removeMemberLike(memberId, productId);
        
        // 4. 좋아요 개수 캐시 업데이트 (감소)
        Long newCount = cacheService.decrementLikeCount(productId);
        int likeCount;
        
        if (newCount == null || newCount < 0) {
            // 캐시가 없었거나 음수가 된 경우 DB에서 정확한 개수 조회 후 캐시 저장
            likeCount = productLikeRepository.countByProductId(productId);
            cacheService.setLikeCount(productId, likeCount);
        } else {
            likeCount = newCount.intValue();
        }

        // 5. Product 엔티티 업데이트
        product.updateLikeCount(likeCount);

        return ProductLikeDto.Response.builder()
                .productId(productId)
                .liked(false)
                .likeCount(likeCount)
                .build();
    }

    /**
     * 좋아요 상태 조회 (Cache Aside Pattern)
     */
    public ProductLikeDto.StatusResponse getLikeStatus(Long memberId, Long productId) {
        // 1. 캐시에서 조회 시도
        Boolean cachedLiked = cacheService.isMemberLikedProduct(memberId, productId);
        
        boolean liked;
        
        if (cachedLiked == null) {
            // 2. 캐시 미스 - DB에서 조회
            liked = productLikeRepository.existsByMemberIdAndProductId(memberId, productId);
            
            // 3. 회원의 전체 좋아요 목록을 캐시에 저장 (워밍업)
            Set<Long> likedProductIds = productLikeRepository.findByMemberId(memberId)
                    .stream()
                    .map(productLike -> productLike.getProduct().getId())
                    .collect(Collectors.toSet());
            
            cacheService.setMemberLikedProducts(memberId, likedProductIds);
            
            log.debug("좋아요 상태 캐시 미스 - DB 조회 및 캐시 저장: memberId={}, productId={}, liked={}", 
                      memberId, productId, liked);
        } else {
            // 캐시 히트
            liked = cachedLiked;
            log.debug("좋아요 상태 캐시 히트: memberId={}, productId={}, liked={}", 
                      memberId, productId, liked);
        }

        return ProductLikeDto.StatusResponse.builder()
                .productId(productId)
                .liked(liked)
                .build();
    }

    /**
     * 좋아요 개수 조회 (Cache Aside Pattern)
     */
    public int getLikeCount(Long productId) {
        // 1. 캐시에서 조회 시도
        Integer cachedCount = cacheService.getLikeCount(productId);
        
        if (cachedCount != null) {
            // 캐시 히트
            log.debug("좋아요 개수 캐시 히트: productId={}, count={}", productId, cachedCount);
            return cachedCount;
        }
        
        // 2. 캐시 미스 - DB에서 조회
        int dbCount = productLikeRepository.countByProductId(productId);
        
        // 3. 캐시에 저장
        cacheService.setLikeCount(productId, dbCount);
        
        log.debug("좋아요 개수 캐시 미스 - DB 조회 및 캐시 저장: productId={}, count={}", 
                  productId, dbCount);
        
        return dbCount;
    }

    /**
     * 캐시 워밍업 - 특정 회원의 좋아요 목록
     */
    @Transactional(readOnly = true)
    public void warmupMemberLikeCache(Long memberId) {
        Set<Long> likedProductIds = productLikeRepository.findByMemberId(memberId)
                .stream()
                .map(productLike -> productLike.getProduct().getId())
                .collect(Collectors.toSet());
        
        cacheService.setMemberLikedProducts(memberId, likedProductIds);
        log.info("회원 좋아요 캐시 워밍업 완료: memberId={}, count={}", memberId, likedProductIds.size());
    }

    /**
     * 캐시 무효화 - 특정 상품
     */
    public void invalidateProductCache(Long productId) {
        cacheService.invalidateProductCache(productId);
        log.info("상품 캐시 무효화: productId={}", productId);
    }

    /**
     * 캐시 무효화 - 특정 회원
     */
    public void invalidateMemberCache(Long memberId) {
        cacheService.invalidateMemberCache(memberId);
        log.info("회원 캐시 무효화: memberId={}", memberId);
    }
}
