package com.usinsa.backend.domain.order.entity;

public enum OrderStatus {
    CREATED,        // 생성됨 (주문 생성)
    PAYMENT_READY,  // 결제 준비 (결제 준비 완료)
    PAYMENT_COMPLETED,  // 결제 완료 (결제 승인 완료)
    CANCELLED       // 취소됨 (주문 취소 또는 결제 취소)
}
