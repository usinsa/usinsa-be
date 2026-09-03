package com.usinsa.backend.domain.search.vector.init;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * product_embedding 테이블에 HNSW 인덱스를 생성한다.
 *
 * ddl-auto:create가 테이블/컬럼은 만들어주지만 pgvector 인덱스는 만들지 못하기 때문에,
 * 앱 기동 시점(스키마 생성 이후)에 CommandLineRunner로 별도 생성한다.
 * BaseInitData(Order 1)보다 먼저 실행되도록 Order(0)으로 지정 - 인덱스는 데이터와 무관하게
 * 스키마가 준비되는 즉시 만들어두는 게 자연스럽다.
 */
@Slf4j
@Component
@Order(0)
@RequiredArgsConstructor
public class ProductEmbeddingIndexInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        try {
            // cosine distance(<=>) 기준 HNSW 인덱스. Hybrid Search의 유사도 계산 연산자와 일치시켜야 한다.
            jdbcTemplate.execute("""
                    CREATE INDEX IF NOT EXISTS idx_product_embedding_hnsw
                    ON product_embedding
                    USING hnsw (embedding vector_cosine_ops)
                    """);
            log.info("product_embedding HNSW 인덱스 준비 완료");
        } catch (Exception e) {
            // 인덱스가 없어도 벡터 검색 자체는 동작하므로(느릴 뿐) 앱 기동을 막지 않는다.
            log.warn("product_embedding HNSW 인덱스 생성 실패 - 벡터 검색 성능에 영향이 있을 수 있습니다: {}", e.getMessage());
        }
    }
}
