package com.usinsa.backend.domain.search.vector.repository;

import com.usinsa.backend.domain.search.vector.entity.ProductEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductEmbeddingRepository extends JpaRepository<ProductEmbedding, Long> {

    /**
     * pgvector cosine distance(<=>) 기반 최근접 이웃 검색.
     * queryVector는 "[0.1,0.2,...]" 형태의 pgvector 리터럴 문자열로 전달한다
     * (float[]를 JDBC 파라미터로 직접 바인딩하면 Hibernate가 네이티브 쿼리에서는
     *  타입을 못 찾으므로, 문자열로 만들어 CAST(... AS vector)로 캐스팅한다).
     * similarity = 1 - cosine distance (1에 가까울수록 유사).
     */
    @Query(value = """
            SELECT product_id AS productId,
                   1 - (embedding <=> CAST(:queryVector AS vector)) AS similarity
            FROM product_embedding
            ORDER BY embedding <=> CAST(:queryVector AS vector)
            LIMIT :topK
            """, nativeQuery = true)
    List<NearestProductProjection> findNearest(@Param("queryVector") String queryVector,
                                                @Param("topK") int topK);

    interface NearestProductProjection {
        Long getProductId();
        Double getSimilarity();
    }
}
