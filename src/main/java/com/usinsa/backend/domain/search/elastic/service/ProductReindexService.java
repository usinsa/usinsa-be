package com.usinsa.backend.domain.search.elastic.service;

import com.usinsa.backend.domain.product.entity.Product;
import com.usinsa.backend.domain.product.repository.ProductRepository;
import com.usinsa.backend.domain.search.elastic.event.ProductSavedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductReindexService {

    private final ProductRepository productRepository;
    private final ApplicationEventPublisher eventPublisher;

    // 전체 reindex
    public int reindexAll() {
        List<Product> all = productRepository.findAll();

        all.forEach(p ->
                eventPublisher.publishEvent(new ProductSavedEvent(p))
        );

        return all.size();
    }
}