package com.usinsa.backend.domain.search.vector.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class VectorSearchResultDto {
    private Long productId;
    private double similarity; // cosine similarity, 0~1
}
