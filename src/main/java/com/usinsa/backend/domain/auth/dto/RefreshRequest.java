package com.usinsa.backend.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(@Email String email, @NotBlank String refreshToken) {}