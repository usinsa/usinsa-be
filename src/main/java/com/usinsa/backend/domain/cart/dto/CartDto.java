package com.usinsa.backend.domain.cart.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

public class CartDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateReq {
        private Long productOptionId;
        private Long memberId;
        private int count;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GuestCreateReq {
        private Long productOptionId;
        private int count;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateReq {
        private int count;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private Long id;
        private Long productOptionId;
        private Long memberId;
        private String sessionId;
        private int count;
        private boolean guest; // isGuest 작명시 JSON 직렬화 문제발생 (Jackson의 getter기반)

        // 프론트엔드 표시용 상품 정보
        private ProductInfo productInfo;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProductInfo {
        private Long productId;
        private String productName;
        private String brandName;
        private Long price;
        private String optionName;
        private Integer stock;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MergeRequest {
        private Long memberId;
    }
}