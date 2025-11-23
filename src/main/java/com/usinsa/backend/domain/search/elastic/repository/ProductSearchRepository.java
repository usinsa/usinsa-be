package com.usinsa.backend.domain.search.elastic.repository;

import com.usinsa.backend.domain.search.elastic.document.ProductDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface ProductSearchRepository extends ElasticsearchRepository<ProductDocument, Long> {
    List<ProductDocument> findByNameContainingIgnoreCase(String name);
}