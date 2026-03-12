package com.usinsa.backend.domain.search.result.service;

import com.usinsa.backend.domain.search.history.service.SearchHistoryService;
import com.usinsa.backend.domain.search.port.ProductSearchPort;
import com.usinsa.backend.domain.search.result.dto.ProductSearchDto;
import com.usinsa.backend.domain.search.trend.service.SearchTrendService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchResultService {

    private final ProductSearchPort productSearchPort;   // ES or ZincSearch
    private final SearchHistoryService searchHistoryService;
    private final SearchTrendService searchTrendService;

    public List<ProductSearchDto> searchProducts(Long userId, String keyword) {
        if (keyword == null || keyword.isBlank()) return List.of();

        List<ProductSearchDto> results = productSearchPort.search(keyword.trim());

        if (userId != null) {
            searchHistoryService.saveUserSearch(userId, keyword);
        }
        searchTrendService.recordKeyword(keyword);

        return results;
    }
}
