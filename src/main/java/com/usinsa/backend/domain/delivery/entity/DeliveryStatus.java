package com.usinsa.backend.domain.delivery.entity;

public enum DeliveryStatus {
    PAYMENT_COMPLETED,  // 결제 완료
    READY,      // 배송 준비 중
    IN_TRANSIT, // 배송 중
    DELIVERED,  // 배송 완료
    CANCELLED   // 배송 취소
}