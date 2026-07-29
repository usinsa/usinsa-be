package com.usinsa.backend.domain.search.rag.controller;

import com.usinsa.backend.domain.search.rag.dto.RagResponseDto;
import com.usinsa.backend.domain.search.rag.service.RagRecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * SearchResultController(/api/v1/search)와 같은 상위 경로 아래
 * /rag 서브 경로로 둬서, 기존 키워드 검색 API와 계열이 같음을 드러낸다.
 */
@RestController
@RequestMapping("/api/v1/search/rag")
@RequiredArgsConstructor
public class RagSearchController {

    private final RagRecommendationService ragRecommendationService;

    // RAG 기반 상품 추천 (Hybrid Search + Gemini)
    @GetMapping
    public RagResponseDto recommend(@RequestParam String keyword) {
        return ragRecommendationService.recommend(keyword);
    }
}
