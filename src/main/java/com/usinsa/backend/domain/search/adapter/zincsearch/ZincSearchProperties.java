package com.usinsa.backend.domain.search.adapter.zincsearch;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Profile("prod")
@ConfigurationProperties(prefix = "zincsearch")
public class ZincSearchProperties {
    private String url;
    private String index;
    private String username;
    private String password;
}
