package com.usinsa.backend.domain.search.embedding.init;

import com.usinsa.backend.domain.product.entity.Product;
import com.usinsa.backend.domain.product.repository.ProductRepository;
import com.usinsa.backend.domain.search.embedding.service.ProductEmbeddingService;
import com.usinsa.backend.domain.search.vector.repository.ProductEmbeddingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * ProductIndexer(ES/Zinc 인덱싱)와 동일한 목적의 pgvector 버전.
 *
 * BaseInitData/일반 상품 저장 로직이 이벤트를 발행하지 않고 Repository로 직접 저장하는
 * 경우(대표적으로 시드 데이터)에는 ProductEmbeddingEventListener가 절대 호출되지 않아
 * 임베딩이 영원히 비어있게 된다. 앱 기동 시 "임베딩 없는 상품"을 찾아 한 번 채워준다.
 *
 * BaseInitData(Order 1)가 상품을 만든 뒤에 실행되어야 하므로 Order(2)로 지정한다.
 */
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class ProductEmbeddingIndexer implements CommandLineRunner {

    private final ProductRepository productRepository;
    private final ProductEmbeddingRepository productEmbeddingRepository;
    private final ProductEmbeddingService productEmbeddingService;

    @Override
    public void run(String... args) {
        List<Product> products = productRepository.findAll();
        long embeddedCount = productEmbeddingRepository.count();

        if (embeddedCount >= products.size()) {
            log.info("모든 상품({}개)에 임베딩이 이미 존재합니다. 백필을 건너뜁니다.", products.size());
            return;
        }

        log.info("상품 임베딩 백필 시작: 전체 {}개 중 {}개만 임베딩 존재", products.size(), embeddedCount);

        int success = 0;
        int failed = 0;
        for (Product product : products) {
            if (productEmbeddingRepository.existsById(product.getId())) {
                continue;
            }
            try {
                productEmbeddingService.saveOrUpdate(product);
                success++;
            } catch (Exception e) {
                failed++;
                log.warn("상품 임베딩 백필 실패: productId={}, error={}", product.getId(), e.getMessage());
            }
        }

        log.info("상품 임베딩 백필 완료: 성공 {}건, 실패 {}건", success, failed);
    }
}
