package com.usinsa.backend.domain.member.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class LoginReqDto {

    @NotEmpty
    private String usinaId;

    @NotEmpty
    private String password;
}
