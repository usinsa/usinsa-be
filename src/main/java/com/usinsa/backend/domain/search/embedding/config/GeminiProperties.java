package com.usinsa.backend.domain.search.embedding.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application-secret.yml 의 gemini.api-key 를 바인딩한다 (기존 값 재사용).
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "gemini")
public class GeminiProperties {
    private String apiKey;
}
