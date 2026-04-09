package com.usinsa.backend.domain.search.adapter.zincsearch;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestTemplate;

@Configuration
@Profile("prod")
@EnableConfigurationProperties(ZincSearchProperties.class)
public class ZincSearchConfig {

    @Bean("zincRestTemplate")
    public RestTemplate zincRestTemplate() {
        return new RestTemplate();
    }
}
