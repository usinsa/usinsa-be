package com.usinsa.backend.domain.search.hybrid.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RankedProductDto {
    private Long productId;
    private String name;
    private String brandName;
    private String categoryName;
    private Long price;
    private double rrfScore; // Reciprocal Rank Fusion 최종 점수
}
