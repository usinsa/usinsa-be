package com.usinsa.backend.domain.search.port;

import com.usinsa.backend.domain.search.result.dto.ProductSearchDto;

import java.util.List;

/**
 * 상품 인덱스 관리 Strategy 인터페이스
 * 구현체: ElasticsearchIndexAdapter (local), ZincSearchIndexAdapter (prod)
 */
public interface ProductIndexPort {
    void save(ProductSearchDto doc);
    void delete(Long productId);
    void saveAll(List<ProductSearchDto> docs);
    long count();
}
