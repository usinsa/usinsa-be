package com.usinsa.backend.global.security.token;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Getter
@Builder
public class TokenMeta {
    private String jti;            // refresh 고유ID
    private Long memberId;
    private String email;
    private List<String> roles;
    private String deviceId;
    private Instant expiresAt;     // refresh 만료
    private Map<String, Object> extra;
}
