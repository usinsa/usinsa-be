package com.usinsa.backend.domain.product.dto;

import com.usinsa.backend.domain.product.entity.ProductOption;
import lombok.*;

public class ProductOptionDto {

    @Getter
    @AllArgsConstructor
    @Builder
    public static class CreateReq {
        private String optionName;
        private Integer stock;
    }

    @Getter
    @AllArgsConstructor
    @Builder
    public static class Response {
        private Long id;
        private String optionName;
        private Integer stock;
        private Long productId;
    }
}