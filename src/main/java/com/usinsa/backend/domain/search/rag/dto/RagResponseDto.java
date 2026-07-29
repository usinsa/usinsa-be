package com.usinsa.backend.domain.search.rag.dto;

import com.usinsa.backend.domain.search.hybrid.dto.RankedProductDto;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class RagResponseDto {
    private String message;                  // Gemini가 생성한 자연어 추천/이유
    private List<RankedProductDto> products; // 근거가 된 실제 상품 목록 (프론트에서 카드로 노출)
}
