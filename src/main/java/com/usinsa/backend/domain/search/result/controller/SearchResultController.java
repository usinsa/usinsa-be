package com.usinsa.backend.domain.search.result.controller;


import com.usinsa.backend.domain.search.result.dto.ProductSearchDto;
import com.usinsa.backend.domain.search.result.service.SearchResultService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchResultController {

    private final SearchResultService searchResultService;
    private final com.usinsa.backend.domain.search.trend.service.SearchTrendService trendService;
    private final com.usinsa.backend.domain.search.history.service.SearchHistoryService historyService;

    // 상품 검색
    @GetMapping
    public List<ProductSearchDto> search(
            @RequestParam(required = false) Long userId,
            @RequestParam String keyword
    ) {
        return searchResultService.searchProducts(userId, keyword);
    }

    // 인기 검색어 조회
    @GetMapping("/trend")
    public Set<String> getTrendingKeywords() {
        return trendService.getTrendingKeywords(10);
    }

    // 사용자별 최근 검색어 조회
    @GetMapping("/history/{userId}")
    public List<String> getUserSearchHistory(@PathVariable Long userId) {
        return historyService.getRecentSearches(userId);
    }
}