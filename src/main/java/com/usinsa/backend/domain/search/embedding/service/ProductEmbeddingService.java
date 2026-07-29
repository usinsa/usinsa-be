package com.usinsa.backend.domain.search.embedding.service;

import com.usinsa.backend.domain.product.entity.Product;
import com.usinsa.backend.domain.search.embedding.client.EmbeddingClient;
import com.usinsa.backend.domain.search.vector.entity.ProductEmbedding;
import com.usinsa.backend.domain.search.vector.repository.ProductEmbeddingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 상품 정보를 기반으로 임베딩을 생성/저장한다.
 * ProductIndexPort(키워드 인덱싱)와 완전히 독립적인 별도 흐름이다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductEmbeddingService {

    private final EmbeddingClient embeddingClient;
    private final ProductEmbeddingRepository productEmbeddingRepository;

    private static final String MODEL_VERSION = "gemini-embedding-001:768";

    @Transactional
    public void saveOrUpdate(Product product) {
        String sourceText = buildSourceText(product);
        float[] vector = embeddingClient.embedDocument(sourceText);

        ProductEmbedding embedding = productEmbeddingRepository.findById(product.getId())
                .orElse(ProductEmbedding.builder().productId(product.getId()).build());

        embedding.update(vector, sourceText, MODEL_VERSION);
        productEmbeddingRepository.save(embedding);

        log.info("상품 임베딩 저장 완료: productId={}", product.getId());
    }

    @Transactional
    public void delete(Long productId) {
        productEmbeddingRepository.deleteById(productId);
    }

    /**
     * 임베딩 생성에 쓸 원문. 상품명/브랜드/카테고리를 합쳐서
     * "화이트 티셔츠 유신사 스탠다드 티셔츠" 같은 형태로 만든다.
     * ProductSearchDto와 동일한 필드 조합을 써서 키워드 검색과 시맨틱 검색의 "의미 기준"을 맞춘다.
     */
    private String buildSourceText(Product product) {
        return String.join(" ",
                product.getName(),
                product.getBrandName(),
                product.getCategory().getName()
        );
    }
}
