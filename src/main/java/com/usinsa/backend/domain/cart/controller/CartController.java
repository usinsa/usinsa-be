package com.usinsa.backend.domain.cart.controller;

import com.usinsa.backend.domain.cart.dto.CartDto;
import com.usinsa.backend.domain.cart.service.CartService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/carts")
public class CartController {

    private final CartService cartService;

    /**
     * 회원 장바구니 생성
     */
    @PostMapping
    public ResponseEntity<CartDto.Response> createCart(@RequestBody CartDto.CreateReq request) {
        return ResponseEntity.ok(cartService.create(request));
    }

    /**
     * 비회원 장바구니 생성 (세션 기반)
     */
    @PostMapping("/guest")
    public ResponseEntity<CartDto.Response> createGuestCart(
            @RequestBody CartDto.GuestCreateReq request,
            HttpSession session
    ) {
        String sessionId = session.getId();
        log.info("비회원 장바구니 생성 요청 - SessionId: {}", sessionId);
        return ResponseEntity.ok(cartService.createGuestCart(request, sessionId));
    }

    /**
     * 장바구니 단건 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<CartDto.Response> getCart(@PathVariable Long id) {
        return ResponseEntity.ok(cartService.findById(id));
    }

    /**
     * 모든 장바구니 조회 (관리자용)
     */
    @GetMapping
    public ResponseEntity<List<CartDto.Response>> getAllCarts() {
        return ResponseEntity.ok(cartService.findAll());
    }

    /**
     * 회원 장바구니 조회
     */
    @GetMapping("/member/{memberId}")
    public ResponseEntity<List<CartDto.Response>> getMemberCarts(@PathVariable Long memberId) {
        return ResponseEntity.ok(cartService.findByMemberId(memberId));
    }

    /**
     * 비회원(세션) 장바구니 조회
     */
    @GetMapping("/guest")
    public ResponseEntity<List<CartDto.Response>> getGuestCarts(HttpSession session) {
        String sessionId = session.getId();
        log.info("비회원 장바구니 조회 요청 - SessionId: {}", sessionId);
        return ResponseEntity.ok(cartService.findBySessionId(sessionId));
    }

    /**
     * 비회원 장바구니를 회원 장바구니로 병합 (로그인 시 호출)
     */
    @PostMapping("/merge/{memberId}")
    public ResponseEntity<List<CartDto.Response>> mergeGuestCart(
            @PathVariable Long memberId,
            HttpSession session
    ) {
        String sessionId = session.getId();
        log.info("장바구니 병합 요청 - SessionId: {}, MemberId: {}", sessionId, memberId);
        return ResponseEntity.ok(cartService.mergeGuestCartToMember(sessionId, memberId));
    }

    /**
     * 장바구니 수량 수정
     */
    @PutMapping("/{id}")
    public ResponseEntity<CartDto.Response> updateCart(
            @PathVariable Long id,
            @RequestBody CartDto.UpdateReq request
    ) {
        return ResponseEntity.ok(cartService.update(id, request));
    }

    /**
     * 장바구니 단건 삭제
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCart(@PathVariable Long id) {
        cartService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 비회원 장바구니 전체 삭제
     */
    @DeleteMapping("/guest")
    public ResponseEntity<Void> deleteGuestCart(HttpSession session) {
        String sessionId = session.getId();
        log.info("비회원 장바구니 삭제 요청 - SessionId: {}", sessionId);
        cartService.deleteGuestCart(sessionId);
        return ResponseEntity.noContent().build();
    }
}