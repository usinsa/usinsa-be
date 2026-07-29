package com.usinsa.backend.domain.search.rag.client;

import com.usinsa.backend.domain.search.hybrid.dto.RankedProductDto;

import java.util.List;

/**
 * Gemini 2.5 Flash 호출 전용 클라이언트.
 * 검색은 하지 않고, 이미 검색된 상품 목록을 근거로 추천 문장만 생성한다.
 */
public interface GeminiClient {
    String generateRecommendation(String userQuery, List<RankedProductDto> candidates);
}
