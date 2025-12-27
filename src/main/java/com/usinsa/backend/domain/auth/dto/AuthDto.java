package com.usinsa.backend.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
    public static class SignupReq {

        @Email
        @NotBlank
        private String email;

        @NotBlank
        @Size(min = 8, max = 20)
        private String password;

        @NotBlank
        private String passwordConfirm;

        @NotBlank
        private String name;

        @NotBlank
        private String nickname;
    }

    @Getter
    @Setter
    public static class RefreshReq {
        private String refreshToken;
    }
}
