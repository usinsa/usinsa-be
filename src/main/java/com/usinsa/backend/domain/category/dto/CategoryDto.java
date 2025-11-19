package com.usinsa.backend.domain.category.dto;

import lombok.*;
import java.util.List;

public class CategoryDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateReq {
        private Long parentId;
        private String name;
    }

    @Getter
    @AllArgsConstructor
    @Builder
    public static class Response {
        private Long id;
        private String name;
        private Long parentId;
        private String parentName;
        private List<String> productNames;
    }
}