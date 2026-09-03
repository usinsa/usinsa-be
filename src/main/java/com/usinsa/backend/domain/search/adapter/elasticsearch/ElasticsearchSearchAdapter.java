package com.usinsa.backend.domain.search.adapter.elasticsearch;

import com.usinsa.backend.domain.search.elastic.document.ProductDocument;
import com.usinsa.backend.domain.search.elastic.repository.ProductSearchRepository;
import com.usinsa.backend.domain.search.port.ProductSearchPort;
import com.usinsa.backend.domain.search.result.dto.ProductSearchDto;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile("dev")
@RequiredArgsConstructor
public class ElasticsearchSearchAdapter implements ProductSearchPort {

    private final ProductSearchRepository productSearchRepository;

    @Override
    public List<ProductSearchDto> search(String keyword) {
        return productSearchRepository
                .searchByKeyword(keyword)
                .stream()
                .map(this::toDto)
                .toList();
    }

    private ProductSearchDto toDto(ProductDocument doc) {
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
