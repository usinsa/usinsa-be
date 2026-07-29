package com.usinsa.backend.domain.search.rag.service;

import com.usinsa.backend.domain.search.hybrid.dto.RankedProductDto;
import com.usinsa.backend.domain.search.hybrid.service.HybridSearchService;
import com.usinsa.backend.domain.search.rag.client.GeminiClient;
import com.usinsa.backend.domain.search.rag.dto.RagResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * RAG 파이프라인 조립 지점.
 * Hybrid Search로 후보를 뽑고, 그 후보만 근거로 Gemini가 추천 문장을 생성한다.
 * 이 클래스 자체는 검색 로직도, Gemini 호출 세부사항도 모른다 - 오직 조립만 담당한다.
 */
@Service
@RequiredArgsConstructor
public class RagRecommendationService {

    private static final int TOP_K = 5;

    private final HybridSearchService hybridSearchService;
    private final GeminiClient geminiClient;

    public RagResponseDto recommend(String userQuery) {
        List<RankedProductDto> candidates = hybridSearchService.search(userQuery, TOP_K);

        String message = candidates.isEmpty()
                ? "검색 결과가 없습니다. 다른 검색어로 다시 시도해주세요."
                : geminiClient.generateRecommendation(userQuery, candidates);

        return RagResponseDto.builder()
                .message(message)
                .products(candidates)
                .build();
    }
}
