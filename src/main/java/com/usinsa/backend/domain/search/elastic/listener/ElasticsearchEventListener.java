package com.usinsa.backend.domain.search.elastic.listener;

import com.usinsa.backend.domain.product.entity.Product;
import com.usinsa.backend.domain.search.elastic.document.ProductDocument;
import com.usinsa.backend.domain.search.elastic.event.ProductDeletedEvent;
import com.usinsa.backend.domain.search.elastic.event.ProductSavedEvent;
import com.usinsa.backend.domain.search.elastic.event.ProductUpdatedEvent;
import com.usinsa.backend.domain.search.elastic.repository.ProductSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import org.springframework.context.annotation.Profile;

@Component
@Profile("dev")
@RequiredArgsConstructor
public class ElasticsearchEventListener {

    private final ProductSearchRepository productSearchRepository;

    @Async
    @EventListener
    public void handleProductSaved(ProductSavedEvent event) {
        Product p = event.product();

        ProductDocument doc = ProductDocument.builder()
                .id(p.getId())
                .name(p.getName())
                .brandName(p.getBrandName())
                .categoryName(p.getCategory().getName())
                .price(p.getPrice())
                .likeCount(p.getLikeCount())
                .clickCount(p.getClickCount())
                .build();

        productSearchRepository.save(doc);
    }

    @Async
    @EventListener
    public void handleProductUpdated(ProductUpdatedEvent event) {
        Product p = event.product();

        ProductDocument doc = ProductDocument.builder()
                .id(p.getId())
                .name(p.getName())
                .brandName(p.getBrandName())
                .categoryName(p.getCategory().getName())
                .price(p.getPrice())
                .likeCount(p.getLikeCount())
                .clickCount(p.getClickCount())
                .build();

        productSearchRepository.save(doc); // update = save 동일
    }

    @Async
    @EventListener
    public void handleProductDeleted(ProductDeletedEvent event) {
        productSearchRepository.deleteById(event.productId());
    }
}