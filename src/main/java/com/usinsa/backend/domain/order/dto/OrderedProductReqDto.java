package com.usinsa.backend.domain.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderedProductReqDto {
    private Long orderId;
    private Long productOptionId;
    private Integer quantity;
}

