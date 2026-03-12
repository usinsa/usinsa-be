package com.usinsa.backend.domain.search.elastic.service;

import com.usinsa.backend.domain.product.repository.ProductRepository;
import com.usinsa.backend.domain.search.port.ProductIndexPort;
import com.usinsa.backend.domain.search.result.dto.ProductSearchDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 검색 인덱스 전체 재색인 (ES or ZincSearch 공통)
 */
@Service
@RequiredArgsConstructor
public class ProductReindexService {

    private final ProductRepository productRepository;
    private final ProductIndexPort productIndexPort;

    public int reindexAll() {
        List<ProductSearchDto> docs = productRepository.findAll().stream()
                .map(p -> ProductSearchDto.builder()
                        .id(p.getId())
                        .name(p.getName())
                        .brandName(p.getBrandName())
                        .categoryName(p.getCategory().getName())
                        .price(p.getPrice())
                        .likeCount(p.getLikeCount())
                        .clickCount(p.getClickCount())
                        .build())
                .toList();

        productIndexPort.saveAll(docs);
        return docs.size();
    }
}
