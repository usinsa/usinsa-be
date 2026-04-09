package com.usinsa.backend.domain.search.port;

import com.usinsa.backend.domain.search.result.dto.ProductSearchDto;

import java.util.List;

/**
 * 상품 검색 Strategy 인터페이스
 * 구현체: ElasticsearchSearchAdapter (local), ZincSearchAdapter (prod)
 */
public interface ProductSearchPort {
    List<ProductSearchDto> search(String keyword);
}
