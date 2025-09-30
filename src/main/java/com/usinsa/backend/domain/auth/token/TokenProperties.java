package com.usinsa.backend.domain.auth.token;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.jwt")
public class TokenProperties {
    private String issuer;
    private Access access = new Access();
    private Refresh refresh = new Refresh();
    @Getter
    @Setter
    public static class Access { private String secret; private Duration expiration; }
    @Getter @Setter public static class Refresh { private String secret; private Duration expiration; }
    // getters/setters
}