package com.usinsa.backend.domain.product.controller;

import com.usinsa.backend.domain.product.service.ProductLikeService;
import com.usinsa.backend.global.dto.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * ProductLike 캐시 관리 컨트롤러
 * 개발/운영 환경에서 캐시 관리를 위한 API 제공
 */
@RestController
@RequestMapping("/api/v1/admin/product-like-cache")
@RequiredArgsConstructor
public class ProductLikeCacheController {

    private final ProductLikeService productLikeService;

    // 특정 회원의 좋아요 캐시 워밍업
    @PostMapping("/warmup/member/{memberId}")
    public ResponseEntity<RsData<String>> warmupMemberCache(@PathVariable Long memberId) {
        productLikeService.warmupMemberLikeCache(memberId);
        return ResponseEntity.ok(RsData.of("S-1", "회원 캐시 워밍업 완료", "memberId: " + memberId));
    }

    // 특정 상품의 캐시 무효화
    @DeleteMapping("/invalidate/product/{productId}")
    public ResponseEntity<RsData<String>> invalidateProductCache(@PathVariable Long productId) {
        productLikeService.invalidateProductCache(productId);
        return ResponseEntity.ok(RsData.of("S-1", "상품 캐시 무효화 완료", "productId: " + productId));
    }

    // 특정 회원의 캐시 무효화
    @DeleteMapping("/invalidate/member/{memberId}")
    public ResponseEntity<RsData<String>> invalidateMemberCache(@PathVariable Long memberId) {
        productLikeService.invalidateMemberCache(memberId);
        return ResponseEntity.ok(RsData.of("S-1", "회원 캐시 무효화 완료", "memberId: " + memberId));
    }
}
