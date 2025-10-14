package com.usinsa.backend.domain.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class SignupReqDto {
    @NotBlank
    private String usinaId;

    @NotBlank
    private String password;

    @NotBlank
    private String name;

    @NotBlank
    private String nickname;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String phone;

    private String profileImage;

}

/*
회원가입 DTO 설계
@Email은 이메일 형식 검증
profileImage는 필수가 아니므로 @NotBlank 제거
 */
