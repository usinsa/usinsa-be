package com.usinsa.backend.domain.cart.controller;

import com.usinsa.backend.domain.cart.dto.CartDto;
import com.usinsa.backend.domain.cart.service.CartService;
import com.usinsa.backend.global.exception.CustomException;
import com.usinsa.backend.global.exception.ErrorCode;
import com.usinsa.backend.global.util.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
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

    private final CartService cartService;

    // ── 회원 장바구니 ──────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<CartDto.Response> createCart(@RequestBody CartDto.CreateReq request) {
        return ResponseEntity.ok(cartService.create(request));
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

    // ── 비회원 장바구니 (guestId 쿠키 기반) ───────────────────────────

    @PostMapping("/guest")
    public ResponseEntity<CartDto.Response> createGuestCart(
            @RequestBody CartDto.GuestCreateReq request,
            HttpServletRequest httpRequest) {
        String guestId = resolveGuestId(httpRequest);
        log.info("비회원 장바구니 생성 - guestId={}", guestId);
        return ResponseEntity.ok(cartService.createGuestCart(request, guestId));
    }

    @GetMapping("/guest")
    public ResponseEntity<List<CartDto.Response>> getGuestCarts(HttpServletRequest httpRequest) {
        String guestId = resolveGuestId(httpRequest);
        log.info("비회원 장바구니 조회 - guestId={}", guestId);
        return ResponseEntity.ok(cartService.findByGuestId(guestId));
    }

    @DeleteMapping("/guest")
    public ResponseEntity<Void> deleteGuestCart(HttpServletRequest httpRequest) {
        String guestId = resolveGuestId(httpRequest);
        log.info("비회원 장바구니 삭제 - guestId={}", guestId);
        cartService.deleteGuestCart(guestId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/guest/{id}")
    public ResponseEntity<CartDto.Response> updateGuestCart(
            @PathVariable Long id,
            @RequestBody CartDto.UpdateReq request,
            HttpServletRequest httpRequest) {
        resolveGuestId(httpRequest); // guestId 검증
        return ResponseEntity.ok(cartService.update(id, request));
    }

    @DeleteMapping("/guest/{id}")
    public ResponseEntity<Void> deleteGuestCartItem(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        resolveGuestId(httpRequest); // guestId 검증
        cartService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ── 병합 ──────────────────────────────────────────────────────────

    @PostMapping("/merge/{memberId}")
    public ResponseEntity<List<CartDto.Response>> mergeGuestCart(
            @PathVariable Long memberId,
            HttpServletRequest httpRequest) {
        String guestId = resolveGuestId(httpRequest);
        log.info("장바구니 병합 - guestId={}, memberId={}", guestId, memberId);
        return ResponseEntity.ok(cartService.mergeGuestCartToMember(guestId, memberId));
    }

    // ── helper ────────────────────────────────────────────────────────

    private String resolveGuestId(HttpServletRequest request) {
        return CookieUtil.resolveGuestId(request)
                .orElseThrow(() -> new CustomException(ErrorCode.SESSION_ID_REQUIRED));
    }
}
