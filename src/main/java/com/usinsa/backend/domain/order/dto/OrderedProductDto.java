package com.usinsa.backend.domain.order.dto;

import lombok.*;

public class OrderedProductDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Request {
        private Long orderId;
        private Long productOptionId;
        private Integer quantity;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private Long id;
        private Long orderId;
        private Long productOptionId;
        private Integer quantity;
    }
}
