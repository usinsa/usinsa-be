package com.usinsa.backend.domain.auth.controller;

import com.usinsa.backend.domain.auth.dto.LoginRequest;
import com.usinsa.backend.domain.auth.dto.RefreshRequest;
import com.usinsa.backend.domain.auth.dto.TokenResponse;
import com.usinsa.backend.domain.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest req) {
        TokenResponse tokens = authService.login(req.idOrEmail(), req.password());
        return ResponseEntity.ok(tokens);
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest req) {
        TokenResponse tokens = authService.refresh(req.email(), req.refreshToken());
        return ResponseEntity.ok(tokens);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
            @AuthenticationPrincipal User principal
    ) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Authorization 헤더가 올바르지 않습니다.");
        }
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증 정보가 없습니다.");
        }
        String token = authHeader.substring(7);
        authService.logout(token, principal.getUsername()); // username=이메일로 사용
        return ResponseEntity.noContent().build();
    }
}

