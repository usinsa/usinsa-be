package com.usinsa.backend.domain.cart.controller;

import com.usinsa.backend.domain.cart.dto.CartDto;
import com.usinsa.backend.domain.cart.service.CartService;
import com.usinsa.backend.global.exception.CustomException;
import com.usinsa.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/carts")
public class CartController {

    private static final String SESSION_HEADER = "X-Session-Id";

    private final CartService cartService;

    @PostMapping
    public ResponseEntity<CartDto.Response> createCart(@RequestBody CartDto.CreateReq request) {
        return ResponseEntity.ok(cartService.create(request));
    }

    /** 비회원 장바구니 생성 — 세션 ID는 X-Session-Id 헤더로 전달 */
    @PostMapping("/guest")
    public ResponseEntity<CartDto.Response> createGuestCart(
            @RequestBody CartDto.GuestCreateReq request,
            @RequestHeader(SESSION_HEADER) String sessionId) {
        log.info("비회원 장바구니 생성 - sessionId={}", sessionId);
        return ResponseEntity.ok(cartService.createGuestCart(request, sessionId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CartDto.Response> getCart(@PathVariable Long id) {
        return ResponseEntity.ok(cartService.findById(id));
    }

    @GetMapping
    public ResponseEntity<List<CartDto.Response>> getAllCarts() {
        return ResponseEntity.ok(cartService.findAll());
    }

    @GetMapping("/member/{memberId}")
    public ResponseEntity<List<CartDto.Response>> getMemberCarts(@PathVariable Long memberId) {
        return ResponseEntity.ok(cartService.findByMemberId(memberId));
    }

    /** 비회원 장바구니 조회 */
    @GetMapping("/guest")
    public ResponseEntity<List<CartDto.Response>> getGuestCarts(
            @RequestHeader(SESSION_HEADER) String sessionId) {
        log.info("비회원 장바구니 조회 - sessionId={}", sessionId);
        return ResponseEntity.ok(cartService.findBySessionId(sessionId));
    }

    /** 비회원 → 회원 장바구니 병합 */
    @PostMapping("/merge/{memberId}")
    public ResponseEntity<List<CartDto.Response>> mergeGuestCart(
            @PathVariable Long memberId,
            @RequestHeader(SESSION_HEADER) String sessionId) {
        log.info("장바구니 병합 - sessionId={}, memberId={}", sessionId, memberId);
        return ResponseEntity.ok(cartService.mergeGuestCartToMember(sessionId, memberId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CartDto.Response> updateCart(
            @PathVariable Long id,
            @RequestBody CartDto.UpdateReq request) {
        return ResponseEntity.ok(cartService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCart(@PathVariable Long id) {
        cartService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /** 비회원 장바구니 전체 삭제 */
    @DeleteMapping("/guest")
    public ResponseEntity<Void> deleteGuestCart(
            @RequestHeader(SESSION_HEADER) String sessionId) {
        log.info("비회원 장바구니 삭제 - sessionId={}", sessionId);
        cartService.deleteGuestCart(sessionId);
        return ResponseEntity.noContent().build();
    }
}
