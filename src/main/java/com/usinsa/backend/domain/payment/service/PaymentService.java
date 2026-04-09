package com.usinsa.backend.domain.payment.service;

import com.usinsa.backend.domain.order.entity.Order;
import com.usinsa.backend.domain.order.entity.OrderedProduct;
import com.usinsa.backend.domain.order.repository.OrderRepository;
import com.usinsa.backend.domain.order.service.OrderService;
import com.usinsa.backend.domain.payment.dto.KakaoPayDto;
import com.usinsa.backend.domain.payment.store.PaymentTidStore;
import com.usinsa.backend.global.exception.CustomException;
import com.usinsa.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 결제 통합 서비스
 * 주문과 카카오페이 결제를 연동하는 비즈니스 로직 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final KakaoPayService kakaoPayService;
    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final PaymentTidStore paymentTidStore;

    private static final String FRONT_BASE_URL = "http://localhost:5173";

    /**
     * 결제 준비
     * @param orderId 주문 ID
     * @param memberId 인증된 회원 ID (보안 검증용)
     * @return 결제 준비 응답
     */
    @Transactional
    public KakaoPayDto.ReadyResponse preparePayment(Long orderId, Long memberId) {
        // 주문 조회 및 검증
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));
        
        // 주문 소유권 검증 (본인의 주문인지 확인)
        validateOrderOwnership(order, memberId);
        
        // 이미 결제 준비가 완료된 주문인지 확인
        if (paymentTidStore.exists(orderId)) {
            throw new CustomException(ErrorCode.PAYMENT_ALREADY_COMPLETED);
        }

        // 주문 상품 정보 조회
        if (order.getOrderedProducts().isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        // 주문 상품 정보로 결제 정보 생성
        String itemName = generateItemName(order);
        int quantity = calculateTotalQuantity(order);
        int totalAmount = calculateTotalAmount(order);

        // 결제 준비 요청 생성
        KakaoPayDto.ReadyRequest request = KakaoPayDto.ReadyRequest.builder()
                .cid(kakaoPayService.getCid())
                .partnerOrderId(String.valueOf(orderId))
                .partnerUserId(String.valueOf(order.getMember().getId()))
                .itemName(itemName)
                .quantity(quantity)
                .totalAmount(totalAmount)
                .taxFreeAmount(0)
                .vatAmount(totalAmount / 11) // 부가세 10%
                .approvalUrl(FRONT_BASE_URL + "/payment/success?orderId=" + orderId)
                .cancelUrl(FRONT_BASE_URL + "/payment/cancel?orderId=" + orderId)
                .failUrl(FRONT_BASE_URL + "/payment/fail?orderId=" + orderId)
                .build();

        // 카카오페이 결제 준비
        KakaoPayDto.ReadyResponse response = kakaoPayService.ready(request);
        
        // TID 저장 (Redis)
        paymentTidStore.save(orderId, response.getTid());
        
        // 주문 상태를 결제 준비로 변경
        orderService.updateToPaymentReady(orderId);
        
        log.info("결제 준비 완료: 주문번호={}, 회원ID={}, TID={}", orderId, memberId, response.getTid());
        
        return response;
    }

    /**
     * 결제 승인
     * @param orderId 주문 ID
     * @param pgToken 결제 승인 토큰
     * @param memberId 인증된 회원 ID (보안 검증용)
     * @return 결제 승인 응답
     */
    @Transactional
    public KakaoPayDto.ApproveResponse approvePayment(Long orderId, String pgToken, Long memberId) {
        // 주문 조회
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));
        
        // 주문 소유권 검증 (본인의 주문인지 확인)
        validateOrderOwnership(order, memberId);
        
        // TID 조회
        String tid = paymentTidStore.find(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_TID_NOT_FOUND));

        // 결제 승인 요청 생성
        KakaoPayDto.ApproveRequest request = KakaoPayDto.ApproveRequest.builder()
                .cid(kakaoPayService.getCid())
                .tid(tid)
                .partnerOrderId(String.valueOf(orderId))
                .partnerUserId(String.valueOf(order.getMember().getId()))
                .pgToken(pgToken)
                .build();

        try {
            // 카카오페이 결제 승인
            KakaoPayDto.ApproveResponse response = kakaoPayService.approve(request);
            
            // 결제 완료 후처리
            orderService.updateToPaymentCompleted(orderId);
            
            // TID 삭제 (결제 완료 후 더 이상 필요 없음)
            paymentTidStore.delete(orderId);
            
            log.info("결제 승인 완료: 주문번호={}, 회원ID={}, 결제금액={}", 
                    orderId, memberId, response.getAmount().getTotal());
            
            return response;
            
        } catch (Exception e) {
            log.error("결제 승인 실패: 주문번호={}, 회원ID={}, 오류={}", orderId, memberId, e.getMessage());
            throw new CustomException(ErrorCode.PAYMENT_FAILED);
        }
    }

    /**
     * 결제 취소
     * @param orderId 주문 ID
     * @param memberId 인증된 회원 ID (보안 검증용)
     * @return 결제 취소 응답
     */
    @Transactional
    public KakaoPayDto.CancelResponse cancelPayment(Long orderId, Long memberId) {
        // 주문 조회
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));
        
        // 주문 소유권 검증 (본인의 주문인지 확인)
        validateOrderOwnership(order, memberId);
        
        // TID 조회 (Redis에 없으면 결제 준비 전이거나 이미 완료된 주문)
        String tid = paymentTidStore.find(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_TID_NOT_FOUND));

        // 취소 금액 계산
        int cancelAmount = calculateTotalAmount(order);

        // 결제 취소 요청 생성
        KakaoPayDto.CancelRequest request = KakaoPayDto.CancelRequest.builder()
                .cid(kakaoPayService.getCid())
                .tid(tid)
                .cancelAmount(cancelAmount)
                .cancelTaxFreeAmount(0)
                .cancelVatAmount(cancelAmount / 11) // 부가세 10%
                .build();

        try {
            // 카카오페이 결제 취소
            KakaoPayDto.CancelResponse response = kakaoPayService.cancel(request);
            
            // 결제 취소 후처리
            orderService.updateToCancelled(orderId);
            
            // TID 삭제
            paymentTidStore.delete(orderId);
            
            log.info("결제 취소 완료: 주문번호={}, 회원ID={}, 취소금액={}", orderId, memberId, cancelAmount);
            
            return response;
            
        } catch (Exception e) {
            log.error("결제 취소 실패: 주문번호={}, 회원ID={}, 오류={}", orderId, memberId, e.getMessage());
            throw new CustomException(ErrorCode.PAYMENT_CANCEL_FAILED);
        }
    }

    /**
     * 주문 소유권 검증
     * 인증된 회원이 해당 주문의 소유자인지 확인
     */
    private void validateOrderOwnership(Order order, Long memberId) {
        if (!order.getMember().getId().equals(memberId)) {
            log.warn("주문 소유권 검증 실패: 주문ID={}, 주문소유자={}, 요청자={}", 
                    order.getId(), order.getMember().getId(), memberId);
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
    }

    /**
     * 상품명 생성 (첫 번째 상품명 외 N건)
     */
    private String generateItemName(Order order) {
        OrderedProduct firstProduct = order.getOrderedProducts().get(0);
        String firstProductName = firstProduct.getProductOption().getProduct().getName();
        
        int additionalCount = order.getOrderedProducts().size() - 1;
        if (additionalCount > 0) {
            return firstProductName + " 외 " + additionalCount + "건";
        }
        return firstProductName;
    }

    /**
     * 총 수량 계산
     */
    private int calculateTotalQuantity(Order order) {
        return order.getOrderedProducts().stream()
                .mapToInt(OrderedProduct::getQuantity)
                .sum();
    }

    /**
     * 총 금액 계산
     * ProductOption은 가격 정보가 없으므로 Product의 가격을 사용
     */
    private int calculateTotalAmount(Order order) {
        return order.getOrderedProducts().stream()
                .mapToInt(orderedProduct -> {
                    Long productPrice = orderedProduct.getProductOption().getProduct().getPrice();
                    Integer quantity = orderedProduct.getQuantity();
                    return productPrice.intValue() * quantity;
                })
                .sum();
    }
}
