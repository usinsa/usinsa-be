package com.usinsa.backend.domain.auth.token;

import lombok.Builder;
import lombok.Getter;

/**
 * Access Token과 Refresh Token을 함께 담는 객체
 */
@Getter
@Builder
public class TokenPair {
    
    /**
     * Access Token
     */
    private String accessToken;
    
    /**
     * Refresh Token
     */
    private String refreshToken;
    
    /**
     * Access Token 만료 시간 (Epoch 초)
     */
    private long accessExpEpochSec;
    
    /**
     * Refresh Token 만료 시간 (Epoch 초)
     */
    private long refreshExpEpochSec;
}
