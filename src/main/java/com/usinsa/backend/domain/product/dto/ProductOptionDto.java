package com.usinsa.backend.domain.product.dto;

import com.usinsa.backend.domain.product.entity.ProductOption;
import lombok.*;

public class ProductOptionDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateReq {
        private String optionName;
        private Integer stock;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private Long id;
        private String optionName;
        private Integer stock;
        private Long productId;

        public static Response fromEntity(ProductOption option) {
            return Response.builder()
                    .id(option.getId())
                    .optionName(option.getOptionName())
                    .stock(option.getStock())
                    .productId(option.getProduct().getId())
                    .build();
        }
    }
}