package com.usinsa.backend.domain.payment.controller;

import com.usinsa.backend.domain.payment.dto.KakaoPayDto;
import com.usinsa.backend.domain.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
@Tag(name = "Payment", description = "결제 관련 API")
public class KakaoPayController {

    private final PaymentService paymentService;

    /**
     * 카카오페이 결제 준비
     * 인증된 사용자만 본인의 주문에 대해 결제를 준비할 수 있습니다.
     */
    @PostMapping("/kakao-pay/{orderId}/ready")
    @Operation(summary = "결제 준비", description = "주문에 대한 카카오페이 결제를 준비합니다. (인증 필요)")
    public ResponseEntity<KakaoPayDto.ReadyResponse> ready(
            @PathVariable Long orderId,
            Authentication authentication) {
        
        Long memberId = getMemberIdFromAuthentication(authentication);
        KakaoPayDto.ReadyResponse response = paymentService.preparePayment(orderId, memberId);
        return ResponseEntity.ok(response);
    }

    /**
     * 카카오페이 결제 승인
     * 인증된 사용자만 본인의 주문에 대해 결제를 승인할 수 있습니다.
     */
    @PostMapping("/kakao-pay/{orderId}/approve")
    @Operation(summary = "결제 승인", description = "카카오페이 결제를 승인합니다. (인증 필요)")
    public ResponseEntity<KakaoPayDto.ApproveResponse> approve(
            @PathVariable Long orderId,
            @RequestParam String pgToken,
            Authentication authentication) {
        
        Long memberId = getMemberIdFromAuthentication(authentication);
        KakaoPayDto.ApproveResponse response = paymentService.approvePayment(orderId, pgToken, memberId);
        return ResponseEntity.ok(response);
    }

    /**
     * 카카오페이 결제 취소
     * 인증된 사용자만 본인의 주문에 대해 결제를 취소할 수 있습니다.
     */
    @PostMapping("/kakao-pay/{orderId}/cancel")
    @Operation(summary = "결제 취소", description = "카카오페이 결제를 취소합니다. (인증 필요)")
    public ResponseEntity<KakaoPayDto.CancelResponse> cancel(
            @PathVariable Long orderId,
            Authentication authentication) {
        
        Long memberId = getMemberIdFromAuthentication(authentication);
        KakaoPayDto.CancelResponse response = paymentService.cancelPayment(orderId, memberId);
        return ResponseEntity.ok(response);
    }

    /**
     * Authentication 객체에서 회원 ID 추출
     * JwtAuthenticationFilter에서 principal에 memberId를 Long 타입으로 저장
     */
    private Long getMemberIdFromAuthentication(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalStateException("인증 정보가 없습니다.");
        }
        return (Long) authentication.getPrincipal();
    }
}
