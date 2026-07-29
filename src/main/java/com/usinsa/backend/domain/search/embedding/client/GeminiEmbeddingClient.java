package com.usinsa.backend.domain.search.embedding.client;

import com.usinsa.backend.domain.search.embedding.config.GeminiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

/**
 * Gemini Embedding API(gemini-embedding-001) 호출 구현체.
 * Spring AI 미사용 원칙에 따라 WebClient로 직접 REST 호출한다.
 *
 * 주의: outputDimensionality=768(비-3072) 요청 시 Gemini가 벡터를 정규화해서 주지 않는다.
 * cosine similarity(pgvector `<=>`)가 정확히 동작하려면 L2 정규화가 필수라서 여기서 직접 처리한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiEmbeddingClient implements EmbeddingClient {

    private static final String MODEL = "gemini-embedding-001";
    private static final int OUTPUT_DIMENSIONALITY = 768;

    private final WebClient geminiWebClient;
    private final GeminiProperties geminiProperties;

    @Override
    public float[] embedDocument(String text) {
        return embed(text, "RETRIEVAL_DOCUMENT");
    }

    @Override
    public float[] embedQuery(String text) {
        return embed(text, "RETRIEVAL_QUERY");
    }

    @SuppressWarnings("unchecked")
    private float[] embed(String text, String taskType) {
        Map<String, Object> requestBody = Map.of(
                "content", Map.of("parts", List.of(Map.of("text", text))),
                "taskType", taskType,
                "outputDimensionality", OUTPUT_DIMENSIONALITY
        );

        Map<String, Object> response = geminiWebClient.post()
                .uri("/v1beta/models/{model}:embedContent", MODEL)
                .header("x-goog-api-key", geminiProperties.getApiKey())
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null || !response.containsKey("embedding")) {
            throw new IllegalStateException("Gemini Embedding 응답이 비어있습니다: " + text);
        }

        Map<String, Object> embeddingObj = (Map<String, Object>) response.get("embedding");
        List<Double> values = (List<Double>) embeddingObj.get("values");

        float[] vector = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            vector[i] = values.get(i).floatValue();
        }

        return normalize(vector);
    }

    /**
     * L2 정규화: vector / ||vector||
     * 3072차원이 아닌 출력은 Gemini가 정규화하지 않으므로 직접 처리해야 cosine similarity가 정확해진다.
     */
    private float[] normalize(float[] vector) {
        double sumSquares = 0.0;
        for (float v : vector) {
            sumSquares += (double) v * v;
        }
        double norm = Math.sqrt(sumSquares);
        if (norm == 0.0) {
            return vector;
        }
        float[] normalized = new float[vector.length];
        for (int i = 0; i < vector.length; i++) {
            normalized[i] = (float) (vector[i] / norm);
        }
        return normalized;
    }
}
