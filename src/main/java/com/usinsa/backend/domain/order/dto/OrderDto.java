package com.usinsa.backend.domain.order.dto;


import com.usinsa.backend.domain.order.entity.OrderStatus;
import lombok.*;

public class OrderDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateReq {
        private Long memberId;
        private String receiverAddress;
        private String receiverName;
        private String receiverPhone;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateReq {
        private String receiverAddress;
        private String receiverName;
        private String receiverPhone;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private Long id;
        private Long memberId;
        private String receiverAddress;
        private String receiverName;
        private String receiverPhone;
        private OrderStatus status;
    }
}