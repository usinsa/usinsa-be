package com.usinsa.backend.domain.search.embedding.client;

/**
 * 텍스트 -> 768차원 벡터 변환 계약.
 * 구현체는 Gemini Embedding API(WebClient)를 직접 호출한다 (Spring AI 미사용).
 *
 * 문서(상품)와 질의(검색어)는 Gemini 내부에서 서로 다른 taskType으로 최적화되므로
 * 메서드를 분리한다 - 하나로 합치면 비대칭 최적화의 이점을 잃는다.
 */
public interface EmbeddingClient {
    float[] embedDocument(String text);
    float[] embedQuery(String text);
}
