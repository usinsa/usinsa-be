package com.usinsa.backend.domain.search.elastic.repository;

import com.usinsa.backend.domain.search.elastic.document.ProductDocument;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface ProductSearchRepository extends ElasticsearchRepository<ProductDocument, Long> {

    /**
     * 기존 findByNameContainingIgnoreCase(wildcard 기반)는 검색어에 공백이 있으면
     * "Cannot constructQuery ... Use expression or multiple clauses instead" 예외가 난다
     * (Spring Data Elasticsearch가 wildcard 쿼리 내부의 공백을 금지함).
     * prod(ZincSearch)가 이미 쓰고 있는 multi_match 방식으로 통일해서 해결한다.
     */
    @Query("""
            {
              "multi_match": {
                "query": "?0",
                "fields": ["name", "brandName", "categoryName"],
                "type": "phrase",
                "zero_terms_query": "none"
              }
            }
            """)
    List<ProductDocument> searchByKeyword(String keyword);
}
