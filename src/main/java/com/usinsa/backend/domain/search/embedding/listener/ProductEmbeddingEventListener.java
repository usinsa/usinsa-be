package com.usinsa.backend.domain.search.embedding.listener;

import com.usinsa.backend.domain.search.elastic.event.ProductDeletedEvent;
import com.usinsa.backend.domain.search.elastic.event.ProductSavedEvent;
import com.usinsa.backend.domain.search.elastic.event.ProductUpdatedEvent;
import com.usinsa.backend.domain.search.embedding.service.ProductEmbeddingService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 상품 이벤트 → 임베딩 생성/삭제 동기화.
 * SearchIndexEventListener(키워드 인덱싱)와 동일하게 @Async 이벤트 리스너로 처리해서
 * 상품 저장 응답 시간에 Gemini API 호출 지연이 영향을 주지 않도록 한다.
 * (UsinsaApplication에 이미 @EnableAsync가 설정되어 있어 별도 설정 불필요)
 */
@Component
@RequiredArgsConstructor
public class ProductEmbeddingEventListener {

    private final ProductEmbeddingService productEmbeddingService;

    @Async
    @EventListener
    public void onProductSaved(ProductSavedEvent event) {
        productEmbeddingService.saveOrUpdate(event.product());
    }

    @Async
    @EventListener
    public void onProductUpdated(ProductUpdatedEvent event) {
        productEmbeddingService.saveOrUpdate(event.product());
    }

    @Async
    @EventListener
    public void onProductDeleted(ProductDeletedEvent event) {
        productEmbeddingService.delete(event.productId());
    }
}
