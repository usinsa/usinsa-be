package com.usinsa.backend.domain.search.result.service;

import com.usinsa.backend.domain.product.dto.ProductDto;
import com.usinsa.backend.domain.product.entity.Product;
import com.usinsa.backend.domain.product.repository.ProductRepository;
import com.usinsa.backend.domain.search.elastic.document.ProductDocument;
import com.usinsa.backend.domain.search.elastic.repository.ProductSearchRepository;
import com.usinsa.backend.domain.search.history.service.SearchHistoryService;
import com.usinsa.backend.domain.search.result.dto.ProductSearchDto;
import com.usinsa.backend.domain.search.trend.service.SearchTrendService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchResultService {

    private final ProductSearchRepository productSearchRepository;
    private final SearchHistoryService searchHistoryService;
    private final SearchTrendService searchTrendService;

    @Transactional(readOnly = true)
    public List<ProductSearchDto> searchProducts(Long userId, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }

        // Elasticsearch 검색
        List<ProductDocument> results = productSearchRepository.findByNameContainingIgnoreCase(keyword.trim());

        // 로그인 사용자만 Redis에 검색 기록 저장
        if (userId != null) {
            searchHistoryService.saveUserSearch(userId, keyword);
        }

        // 인기 검색어 트렌드 업데이트
        searchTrendService.recordKeyword(keyword);

        // 결과 DTO 변환
        return results.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private ProductSearchDto toResponse(ProductDocument doc) {
        return ProductSearchDto.builder()
                .id(doc.getId())
                .name(doc.getName())
                .brandName(doc.getBrandName())
                .categoryName(doc.getCategoryName())
                .price(doc.getPrice())
                .likeCount(doc.getLikeCount())
                .clickCount(doc.getClickCount())
                .build();
    }
}