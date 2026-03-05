package com.usinsa.backend.domain.auth.oauth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "oauth")
public class OAuth2Properties {

    /** FE OAuth 콜백 기본 URL (provider가 suffix로 붙음) */
    private String callbackBaseUrl = "http://localhost:5173/oauth/callback";

    /** CORS 허용 Origin 목록 */
    private String[] allowedOrigins = {
        "http://localhost:5173",
        "http://127.0.0.1:5173"
    };
}
