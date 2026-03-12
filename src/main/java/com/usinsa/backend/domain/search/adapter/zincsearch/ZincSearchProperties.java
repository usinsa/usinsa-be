package com.usinsa.backend.domain.search.adapter.zincsearch;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@Profile("prod")
@ConfigurationProperties(prefix = "zincsearch")
public class ZincSearchProperties {
    private String url = "http://localhost:4080";
    private String index = "products";
    private String username = "admin";
    private String password = "Complexpass#123";
}
