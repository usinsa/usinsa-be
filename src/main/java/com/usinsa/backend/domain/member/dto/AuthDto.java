package com.usinsa.backend.domain.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class AuthDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SignupReq {
        @NotBlank
        private String usinaId;

        @NotBlank
        private String password;

        @NotBlank
        private String name;

        @NotBlank
        private String nickname;

        @Email
        @NotBlank
        private String email;

        @NotBlank
        private String phone;

        private String profileImage;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @NotBlank
    public static class LoginReq {
        private String usinaId;
        private String password;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LoginRes {
        private Long memberId;
        private String email;
        private String name;
        private String nickname;
        private String accessToken;
    }
}
