package com.usinsa.backend.domain.delivery.dto;

import com.usinsa.backend.domain.delivery.entity.DeliveryStatus;
import lombok.*;

public class DeliveryDto {

    @Getter
    @AllArgsConstructor
    @Builder
    public static class CreateReq {
        private Long orderId;           // 연관된 주문 ID
        private String trackingNumber;  // 송장번호
        private DeliveryStatus deliveryStatus; // 배송 상태
    }

    @Getter
    @AllArgsConstructor
    @Builder
    public static class Response {
        private Long id;
        private Long orderId;
        private String trackingNumber;
        private DeliveryStatus deliveryStatus;
    }
}