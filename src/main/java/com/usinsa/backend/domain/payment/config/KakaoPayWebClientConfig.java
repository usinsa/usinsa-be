package com.usinsa.backend.domain.payment.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@RequiredArgsConstructor
public class KakaoPayWebClientConfig {

    private final KakaoPayProperties kakaoPayProperties;

    @Bean
    public WebClient kakaoPayWebClient() {
        return WebClient.builder()
                .baseUrl("https://open-api.kakaopay.com")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "SECRET_KEY " + kakaoPayProperties.getSecretKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
