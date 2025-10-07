package com.usinsa.backend.domain.product.dto;

import com.usinsa.backend.domain.product.entity.Product;
import lombok.*;

public class ProductDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateReq {
        private Long categoryId;
        private String name;
        private String brand;
        private Long price;
    }

    @Getter
    @Setter
    @NoArgsConstructor
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

        public static Response fromEntity(Product product) {
            return Response.builder()
                    .id(product.getId())
                    .categoryName(product.getCategory().getName())
                    .name(product.getName())
                    .brandName(product.getBrandName())
                    .price(product.getPrice())
                    .likeCount(product.getLikeCount())
                    .clickCount(product.getClickCount())
                    .build();
        }
    }
}