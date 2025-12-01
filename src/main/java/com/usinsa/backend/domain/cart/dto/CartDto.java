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