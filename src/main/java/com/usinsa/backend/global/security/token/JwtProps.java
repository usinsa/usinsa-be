package com.usinsa.backend.global.security.token;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProps {
    private String secret;
    private int accessExpSeconds;
    private int refreshExpSeconds;

    public void setSecret(String secret) { this.secret = secret; }
    public void setAccessExpSeconds(int accessExpSeconds) { this.accessExpSeconds = accessExpSeconds; }
    public void setRefreshExpSeconds(int refreshExpSeconds) { this.refreshExpSeconds = refreshExpSeconds; }
}
