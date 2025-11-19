package com.usinsa.backend.domain.search.result.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProductSearchDto {
    private Long id;
    private String name;
    private String brandName;
    private String categoryName;
    private Long price;
    private int likeCount;
    private int clickCount;
}
