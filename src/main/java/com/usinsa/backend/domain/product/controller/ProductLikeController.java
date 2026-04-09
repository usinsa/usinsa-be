package com.usinsa.backend.domain.product.controller;

import com.usinsa.backend.domain.product.dto.ProductLikeDto;
import com.usinsa.backend.domain.product.service.ProductLikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductLikeController {

    private final ProductLikeService productLikeService;

    // 좋아요 추가
    @PostMapping("/{productId}/like")
    public ResponseEntity<ProductLikeDto.Response> addLike(
            @PathVariable Long productId,
            @RequestParam Long memberId) {
        ProductLikeDto.Response response = productLikeService.addLike(memberId, productId);
        return ResponseEntity.ok(response);
    }

    // 좋아요 취소
    @DeleteMapping("/{productId}/like")
    public ResponseEntity<ProductLikeDto.Response> removeLike(
            @PathVariable Long productId,
            @RequestParam Long memberId) {
        ProductLikeDto.Response response = productLikeService.removeLike(memberId, productId);
        return ResponseEntity.ok(response);
    }

    // 좋아요 상태 조회
    @GetMapping("/{productId}/like")
    public ResponseEntity<ProductLikeDto.StatusResponse> getLikeStatus(
            @PathVariable Long productId,
            @RequestParam Long memberId) {
        ProductLikeDto.StatusResponse response = productLikeService.getLikeStatus(memberId, productId);
        return ResponseEntity.ok(response);
    }

    // 좋아요 개수 조회
    @GetMapping("/{productId}/like/count")
    public ResponseEntity<Integer> getLikeCount(@PathVariable Long productId) {
        int count = productLikeService.getLikeCount(productId);
        return ResponseEntity.ok(count);
    }
}
