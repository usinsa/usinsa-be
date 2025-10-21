package com.usinsa.backend.global.security.token;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TokenPair {
    private String accessToken;
    private String refreshToken;
    private long accessExpEpochSec;
    private long refreshExpEpochSec;
}