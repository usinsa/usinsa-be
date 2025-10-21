package com.usinsa.backend.domain.auth.dto;

import lombok.*;

public class AuthDto {

    @Getter
    @Setter
    public static class LoginReq {
        private String email;
        private String password;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class LoginRes {
        private Long memberId;
        private String email;
        private String name;
        private String nickname;
        private String accessToken;
        private String refreshToken;
        private long accessTokenExp;
        private long refreshTokenExp;
    }

    @Getter
    @Setter
    public static class RefreshReq {
        private String refreshToken;
    }
}
