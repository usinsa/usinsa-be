package com.usinsa.backend.domain.product.dto;

import lombok.*;

public class ProductDto {

    @Getter
    @AllArgsConstructor
    @Builder
    public static class CreateReq {
        private Long categoryId;
        private String name;
        private String brand;
        private Long price;
    }

    @Getter
    @AllArgsConstructor
    @Builder
    public static class Response {
        private Long id;
        private String categoryName;
        private String name;
        private String brandName;
        private Long price;
        private Integer likeCount;
        private Integer clickCount;
    }
}