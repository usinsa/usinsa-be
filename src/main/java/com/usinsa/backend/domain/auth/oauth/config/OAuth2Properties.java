package com.usinsa.backend.domain.auth.oauth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * OAuth 2.0 관련 설정 프로퍼티
 * application.yml에서 oauth.* 값을 바인딩
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "oauth")
public class OAuth2Properties {
    
    /**
     * FE 리다이렉트 URL
     */
    private String redirectUrl = "http://localhost:5173/oauth/redirect";
    
    /**
     * 허용된 Origin 목록
     */
    private String[] allowedOrigins = {
        "http://localhost:5173",
        "http://127.0.0.1:5173"
    };
}
