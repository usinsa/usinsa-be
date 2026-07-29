package com.usinsa.backend.domain.search.embedding.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * ZincSearchConfig 패턴(RestTemplate 빈 등록)과 동일한 방식으로,
 * Gemini 호출 전용 WebClient를 별도 빈으로 등록한다.
 * dev/prod 모두에서 임베딩/RAG 기능을 쓰므로 @Profile 제한을 두지 않는다.
 */
@Configuration
@EnableConfigurationProperties(GeminiProperties.class)
public class GeminiEmbeddingConfig {

    @Bean("geminiWebClient")
    public WebClient geminiWebClient() {
        return WebClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com")
                .build();
    }
}
