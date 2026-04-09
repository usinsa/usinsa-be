package com.usinsa.backend.domain.auth.controller;

import com.usinsa.backend.domain.auth.dto.AuthDto;
import com.usinsa.backend.domain.auth.service.AuthService;
import com.usinsa.backend.domain.auth.token.TokenPair;
import com.usinsa.backend.global.dto.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "인증 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "일반 로그인")
    @PostMapping("/login")
    public ResponseEntity<RsData<AuthDto.LoginRes>> login(
            @Valid @RequestBody AuthDto.LoginReq body,
            HttpServletRequest req, HttpServletResponse res) {
        return ResponseEntity.ok(RsData.of("S-1", "로그인 성공", authService.login(body, req, res)));
    }

    @Operation(summary = "토큰 갱신 (Refresh 쿠키 사용)")
    @PostMapping("/refresh")
    public ResponseEntity<RsData<TokenPair>> refresh(
            HttpServletRequest req, HttpServletResponse res) {
        return ResponseEntity.ok(RsData.of("S-1", "토큰 갱신 성공", authService.refresh(req, res)));
    }

    @Operation(summary = "로그아웃")
    @PostMapping("/logout")
    public ResponseEntity<RsData<Void>> logout(
            HttpServletRequest req, HttpServletResponse res) {
        authService.logout(req, res);
        return ResponseEntity.ok(RsData.of("S-1", "로그아웃 성공"));
    }

    @Operation(summary = "회원가입")
    @PostMapping("/signup")
    public ResponseEntity<RsData<Void>> signup(@Valid @RequestBody AuthDto.SignupReq body) {
        authService.signup(body);
        return ResponseEntity.ok(RsData.of("S-1", "회원가입 성공"));
    }

    @Operation(summary = "내 정보 조회 (쿠키 인증 상태 확인)")
    @GetMapping("/me")
    public ResponseEntity<RsData<AuthDto.MeRes>> me(
            @AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(RsData.of("S-1", "조회 성공", authService.me(memberId)));
    }
}
