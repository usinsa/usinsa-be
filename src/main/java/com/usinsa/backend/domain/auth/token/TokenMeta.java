package com.usinsa.backend.domain.auth.token;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Refresh Token의 메타 정보
 * Redis에 저장되어 토큰 갱신 시 사용
 */
@Getter
@Builder
public class TokenMeta {
    
    /**
     * Refresh Token의 고유 ID (jti claim)
     */
    private String jti;
    
    /**
     * 회원 ID
     */
    private Long memberId;
    
    /**
     * 회원 이메일
     */
    private String email;
    
    /**
     * 회원 권한 목록
     */
    private List<String> roles;
    
    /**
     * 디바이스 ID (동일 사용자의 여러 디바이스 구분)
     */
    private String deviceId;
    
    /**
     * Refresh Token 만료 시간
     */
    private Instant expiresAt;
    
    /**
     * 추가 정보 (확장 가능)
     */
    private Map<String, Object> extra;
}
