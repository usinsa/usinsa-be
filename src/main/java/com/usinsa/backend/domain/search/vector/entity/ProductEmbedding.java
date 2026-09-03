package com.usinsa.backend.domain.search.vector.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Product와 1:1 관계이지만 별도 테이블로 분리한다.
 * 이유: 임베딩 모델 교체/재계산 시 Product 테이블을 건드리지 않기 위함,
 *       그리고 pgvector 인덱스(HNSW/IVFFlat)를 이 테이블에만 독립적으로 건다.
 */
@Entity
@Table(name = "product_embedding")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductEmbedding {

    @Id
    @Column(name = "product_id") // Product.id 와 동일 값 (FK 겸 PK, IDENTITY 생성 없음)
    private Long productId;

    // Hibernate 네이티브 vector 타입 매핑 (hibernate-vector 모듈).
    // ddl-auto:create 시 Hibernate가 자동으로 `vector(768)` 컬럼을 생성해준다.
    @JdbcTypeCode(SqlTypes.VECTOR)
    @Array(length = 768) // Gemini Embedding 차원 수와 일치해야 함
    @Column(name = "embedding")
    private float[] embedding;

    @Column(name = "source_text", columnDefinition = "TEXT")
    private String sourceText; // 임베딩 생성에 사용한 원문 (상품명+브랜드+카테고리 등, 디버깅/재생성용)

    @Column(name = "model_version", length = 50)
    private String modelVersion; // 예: "gemini-embedding-004", 모델 교체 추적용

    public void update(float[] embedding, String sourceText, String modelVersion) {
        this.embedding = embedding;
        this.sourceText = sourceText;
        this.modelVersion = modelVersion;
    }
}
