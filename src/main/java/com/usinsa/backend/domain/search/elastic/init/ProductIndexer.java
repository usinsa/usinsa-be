package com.usinsa.backend.domain.search.elastic.init;

import com.usinsa.backend.domain.product.repository.ProductRepository;
import com.usinsa.backend.domain.search.elastic.document.ProductDocument;
import com.usinsa.backend.domain.search.elastic.repository.ProductSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductIndexer implements CommandLineRunner {

    private final ProductRepository productRepository;
    private final ProductSearchRepository productSearchRepository;


    @Override
    public void run(String... args) {
        // Elasticsearch 인덱스가 이미 있으면 재인덱싱하지 않음
        long existingCount = productSearchRepository.count();
        if (existingCount > 0) {
            log.info("Elasticsearch 인덱스에 이미 {}개의 상품이 있습니다. 재인덱싱을 건너뜁니다.", existingCount);
            return;
        }

        log.info("Elasticsearch 인덱싱을 시작합니다...");
        var products = productRepository.findAll();
        var docs = products.stream()
                .map(p -> ProductDocument.builder()
                        .id(p.getId())
                        .name(p.getName())
                        .brandName(p.getBrandName())
                        .categoryName(p.getCategory().getName())
                        .price(p.getPrice())
                        .likeCount(p.getLikeCount())
                        .clickCount(p.getClickCount())
                        .build())
                .collect(Collectors.toList());

        productSearchRepository.saveAll(docs);
        log.info("Elasticsearch 인덱싱 완료 ({} 개 상품)", docs.size());
    }
}