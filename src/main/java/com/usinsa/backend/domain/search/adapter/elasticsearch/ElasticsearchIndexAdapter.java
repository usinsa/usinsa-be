package com.usinsa.backend.domain.search.adapter.elasticsearch;

import com.usinsa.backend.domain.search.elastic.document.ProductDocument;
import com.usinsa.backend.domain.search.elastic.repository.ProductSearchRepository;
import com.usinsa.backend.domain.search.port.ProductIndexPort;
import com.usinsa.backend.domain.search.result.dto.ProductSearchDto;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile("dev")
@RequiredArgsConstructor
public class ElasticsearchIndexAdapter implements ProductIndexPort {

    private final ProductSearchRepository productSearchRepository;

    @Override
    public void initIndex() {}

    @Override
    public void save(ProductSearchDto dto) {
        productSearchRepository.save(toDocument(dto));
    }

    @Override
    public void delete(Long productId) {
        productSearchRepository.deleteById(productId);
    }

    @Override
    public void saveAll(List<ProductSearchDto> docs) {
        productSearchRepository.saveAll(docs.stream().map(this::toDocument).toList());
    }

    @Override
    public long count() {
        return productSearchRepository.count();
    }

    private ProductDocument toDocument(ProductSearchDto dto) {
        return ProductDocument.builder()
                .id(dto.getId())
                .name(dto.getName())
                .brandName(dto.getBrandName())
                .categoryName(dto.getCategoryName())
                .price(dto.getPrice())
                .likeCount(dto.getLikeCount())
                .clickCount(dto.getClickCount())
                .build();
    }
}
