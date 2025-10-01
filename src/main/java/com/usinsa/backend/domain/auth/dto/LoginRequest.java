package com.usinsa.backend.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank String idOrEmail, @NotBlank String password) {}