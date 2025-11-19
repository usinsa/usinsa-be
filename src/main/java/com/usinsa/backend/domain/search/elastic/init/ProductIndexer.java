package com.usinsa.backend.domain.search.elastic.init;

import com.usinsa.backend.domain.product.repository.ProductRepository;
import com.usinsa.backend.domain.search.elastic.document.ProductDocument;
import com.usinsa.backend.domain.search.elastic.repository.ProductSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ProductIndexer implements CommandLineRunner {

    private final ProductRepository productRepository;
    private final ProductSearchRepository productSearchRepository;

    @Override
    public void run(String... args) {
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
        System.out.println("Elasticsearch 인덱싱 완료 (" + docs.size() + "개 상품)");
    }
}