package com.usinsa.backend.domain.search.result.service;

import com.usinsa.backend.domain.product.dto.ProductDto;
import com.usinsa.backend.domain.product.entity.Product;
import com.usinsa.backend.domain.product.repository.ProductRepository;
import com.usinsa.backend.domain.search.history.service.SearchHistoryService;
import com.usinsa.backend.domain.search.trend.service.SearchTrendService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchResultService {

    private final ProductRepository productRepository;
    private final SearchHistoryService searchHistoryService;
    private final SearchTrendService searchTrendService;

    @Transactional(readOnly = true)
    public List<ProductDto.Response> searchProducts(Long userId, String keyword) {
        // 상품 검색
        List<Product> results = productRepository.findByNameContaining(keyword);

        // 로그인 사용자만 검색 기록 저장
        if (userId != null) {
            searchHistoryService.saveUserSearch(userId, keyword);
        }

        // 인기 검색어 트렌드 집계
        searchTrendService.recordKeyword(keyword);

        // DTO 변환
        return results.stream()
                .map(this::toProductResDto)
                .collect(Collectors.toList());
    }

    private ProductDto.Response toProductResDto(Product product) {
        return ProductDto.Response.builder()
                .id(product.getId())
                .categoryName(product.getCategory().getName()) // LAZY 초기화 시 session 필요 없음
                .name(product.getName())
                .brandName(product.getBrandName())
                .price(product.getPrice())
                .likeCount(product.getLikeCount())
                .clickCount(product.getClickCount())
                .build();
    }
}