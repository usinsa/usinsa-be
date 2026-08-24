package com.usinsa.backend.domain.search.rag.client;

import com.usinsa.backend.domain.search.embedding.config.GeminiProperties;
import com.usinsa.backend.domain.search.hybrid.dto.RankedProductDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Gemini generateContent API 호출 구현체.
 * 상품 검색은 하지 않는다 - HybridSearchService가 이미 뽑아준 후보만 근거로
 * 추천 문장을 생성한다 (hallucination 방지 원칙).
 * 프롬프트에는 이름/브랜드/가격/카테고리만 넣는다 (RRF 점수 등 내부 값은 제외).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiGenerationClient implements GeminiClient {

    private static final String MODEL = "gemini-flash-latest";

    private final WebClient geminiWebClient;
    private final GeminiProperties geminiProperties;

    @Override
    @SuppressWarnings("unchecked")
    public String generateRecommendation(String userQuery, List<RankedProductDto> candidates) {
        String prompt = buildPrompt(userQuery, candidates);

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt))))
        );

        try {
            Map<String, Object> response = geminiWebClient.post()
                    .uri("/v1beta/models/{model}:generateContent", MODEL)
                    .header("x-goog-api-key", geminiProperties.getApiKey())
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();

            return extractText(response);
        } catch (Exception e) {
            log.warn("Gemini 추천 문구 생성 실패, 상품 목록만 반환합니다: {}", e.getMessage());
            return "지금은 AI 추천 문구를 생성하지 못했어요. 아래 상품 목록을 확인해주세요.";
        }
    }

    /**
     * 프롬프트에는 후보 상품의 이름/브랜드/가격/카테고리만 나열한다.
     * Gemini가 이 목록에 없는 상품을 지어내지 않도록, "아래 목록에 있는 상품만 근거로" 지시를 명시한다.
     */
    private String buildPrompt(String userQuery, List<RankedProductDto> candidates) {
        StringBuilder sb = new StringBuilder();
        sb.append("사용자 검색어: ").append(userQuery).append("\n\n");
        sb.append("아래는 검색된 상품 목록이다. 이 목록에 있는 상품만 근거로 자연스러운 한국어 추천 문장과 ")
          .append("간단한 추천 이유를 작성해라. 목록에 없는 상품을 언급하거나 지어내지 마라.\n\n");

        for (int i = 0; i < candidates.size(); i++) {
            RankedProductDto p = candidates.get(i);
            sb.append(i + 1).append(". ")
              .append(p.getName())
              .append(" / 브랜드: ").append(p.getBrandName())
              .append(" / 카테고리: ").append(p.getCategoryName())
              .append(" / 가격: ").append(p.getPrice()).append("원\n");
        }

        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private String extractText(Map<String, Object> response) {
        if (response == null || !response.containsKey("candidates")) {
            return "";
        }
        List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
        if (candidates.isEmpty()) {
            return "";
        }
        Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
        List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
        return (String) parts.get(0).get("text");
    }
}
