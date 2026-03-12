package com.usinsa.backend.domain.search.listener;

import com.usinsa.backend.domain.product.entity.Product;
import com.usinsa.backend.domain.search.elastic.event.ProductDeletedEvent;
import com.usinsa.backend.domain.search.elastic.event.ProductSavedEvent;
import com.usinsa.backend.domain.search.elastic.event.ProductUpdatedEvent;
import com.usinsa.backend.domain.search.port.ProductIndexPort;
import com.usinsa.backend.domain.search.result.dto.ProductSearchDto;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 상품 이벤트 → 검색 인덱스 동기화 (Strategy: ES or ZincSearch)
 */
@Component
@RequiredArgsConstructor
public class SearchIndexEventListener {

    private final ProductIndexPort productIndexPort;

    @Async
    @EventListener
    public void onProductSaved(ProductSavedEvent event) {
        productIndexPort.save(toDto(event.product()));
    }

    @Async
    @EventListener
    public void onProductUpdated(ProductUpdatedEvent event) {
        productIndexPort.save(toDto(event.product()));
    }

    @Async
    @EventListener
    public void onProductDeleted(ProductDeletedEvent event) {
        productIndexPort.delete(event.productId());
    }

    private ProductSearchDto toDto(Product p) {
        return ProductSearchDto.builder()
                .id(p.getId())
                .name(p.getName())
                .brandName(p.getBrandName())
                .categoryName(p.getCategory().getName())
                .price(p.getPrice())
                .likeCount(p.getLikeCount())
                .clickCount(p.getClickCount())
                .build();
    }
}
