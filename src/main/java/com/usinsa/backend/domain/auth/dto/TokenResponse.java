package com.usinsa.backend.domain.auth.dto;

public record TokenResponse(String accessToken,
                            String refreshToken,
                            long accessTokenExpiresInSeconds,
                            long refreshTokenExpiresInSeconds) {}
