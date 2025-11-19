package com.usinsa.backend.domain.order.dto;

import lombok.*;

public class OrderedProductDto {

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class Request {
        private Long orderId;
        private Long productOptionId;
        private Integer quantity;
    }

    @Getter
    @AllArgsConstructor
    @Builder
    public static class Response {
        private Long id;
        private Long orderId;
        private Long productOptionId;
        private Integer quantity;
    }
}
