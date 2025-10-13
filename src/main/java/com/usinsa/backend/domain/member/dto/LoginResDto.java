package com.usinsa.backend.domain.member.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
public class LoginResDto {
    private Long memberId;

    private String email;

    private String name;

    private String nickname;

    private String accessToken;
}
