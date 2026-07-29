package com.usinsa.backend.domain.search.port;

import com.usinsa.backend.domain.search.vector.dto.VectorSearchResultDto;

import java.util.List;

/**
 * 상품 시맨틱(벡터) 검색 계약
 * 구현체: PgVectorAdapter
 * ProductSearchPort(키워드 검색)와 완전히 독립적인 별도 계약이다.
 */
public interface ProductVectorSearchPort {
    List<VectorSearchResultDto> search(float[] queryVector, int topK);
}
