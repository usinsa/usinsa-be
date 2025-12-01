package com.usinsa.backend.domain.auth.controller;

import com.usinsa.backend.domain.auth.dto.AuthDto;
import com.usinsa.backend.domain.auth.service.AuthService;
import com.usinsa.backend.domain.auth.token.TokenPair;
import com.usinsa.backend.global.dto.RsData;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 인증 관련 API 컨트롤러
 * - 로그인
 * - 토큰 갱신
 * - 로그아웃
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    /**
     * 로그인 API
     *
     * @param reqBody 로그인 요청 (이메일, 비밀번호)
     * @param req     HTTP 요청
     * @param res     HTTP 응답
     * @return 로그인 응답 (회원 정보 + JWT 토큰)
     */
    @PostMapping("/login")
    public ResponseEntity<RsData<AuthDto.LoginRes>> login(
            @Valid @RequestBody AuthDto.LoginReq reqBody,
            HttpServletRequest req,
            HttpServletResponse res) {
        
        AuthDto.LoginRes loginRes = authService.login(reqBody, req, res);
        return ResponseEntity.ok(RsData.of("S-1", "로그인 성공", loginRes));
    }

    /**
     * 토큰 갱신 API
     * Refresh Token을 이용하여 새로운 Access Token 발급
     *
     * @param body 토큰 갱신 요청 (Refresh Token)
     * @param req  HTTP 요청
     * @return 새로운 TokenPair
     */
    @PostMapping("/refresh")
    public ResponseEntity<RsData<TokenPair>> refresh(
            @Valid @RequestBody AuthDto.RefreshReq body,
            HttpServletRequest req) {
        
        TokenPair tokenPair = authService.refresh(body, req);
        return ResponseEntity.ok(RsData.of("S-1", "토큰 갱신 성공", tokenPair));
    }

    /**
     * 로그아웃 API
     * Access Token을 블랙리스트에 등록
     *
     * @param req HTTP 요청
     * @param res HTTP 응답
     * @return 204 No Content
     */
    @PostMapping("/logout")
    public ResponseEntity<RsData<Void>> logout(
            HttpServletRequest req,
            HttpServletResponse res) {
        
        authService.logout(req, res);
        return ResponseEntity.ok(RsData.of("S-1", "로그아웃 성공"));
    }
}
