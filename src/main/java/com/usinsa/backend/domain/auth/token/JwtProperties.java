package com.usinsa.backend.domain.auth.token;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 관련 설정 프로퍼티
 * application-secret.yml에서 jwt.* 값을 바인딩
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    
    /**
     * JWT 서명에 사용할 비밀 키
     */
    private String secret;
    
    /**
     * Access Token 만료 시간 (초 단위)
     */
    private long accessExpireSeconds;
    
    /**
     * Refresh Token 만료 시간 (초 단위)
     */
    private long refreshExpireSeconds;
}
