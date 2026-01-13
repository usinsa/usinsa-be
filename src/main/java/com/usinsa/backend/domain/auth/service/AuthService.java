package com.usinsa.backend.domain.auth.service;

import com.usinsa.backend.domain.auth.dto.AuthDto;
import com.usinsa.backend.domain.auth.token.JwtTokenService;
import com.usinsa.backend.domain.auth.token.TokenPair;
import com.usinsa.backend.domain.member.entity.Member;
import com.usinsa.backend.domain.member.repository.MemberRepository;
import com.usinsa.backend.global.exception.CustomException;
import com.usinsa.backend.global.exception.ErrorCode;
import com.usinsa.backend.global.util.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 인증 관련 비즈니스 로직 처리
 * - 로그인
 * - 토큰 갱신
 * - 로그아웃
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService tokenService;

    /**
     * 로그인
     * 이메일과 비밀번호 검증 후 JWT 토큰 발급
     *
     * @param body 로그인 요청 정보
     * @param req  HTTP 요청
     * @param res  HTTP 응답
     * @return 로그인 응답 (회원 정보 + 토큰)
     */
    @Transactional(readOnly = true)
    public AuthDto.LoginRes login(AuthDto.LoginReq body, HttpServletRequest req, HttpServletResponse res) {
        // 회원 조회
        Member member = memberRepository.findByEmail(body.getEmail())
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        // 비밀번호 검증
        if (!passwordEncoder.matches(body.getPassword(), member.getPassword())) {
            throw new CustomException(ErrorCode.UNAUTHORIZED, "이메일 또는 비밀번호가 일치하지 않습니다.");
        }

        // 디바이스 ID 추출
        String deviceId = CookieUtil.resolveDeviceId(req);

        // 회원 권한 로드 (실제 구현에 맞게 수정 필요)
        List<String> roles = List.of("ROLE_USER");

        // JWT 토큰 발급
        TokenPair tokenPair = tokenService.issueTokens(
                member.getId(),
                member.getEmail(),
                roles,
                deviceId
        );

        log.info("Login successful: memberId={}, email={}", member.getId(), member.getEmail());

        return AuthDto.LoginRes.builder()
                .memberId(member.getId())
                .email(member.getEmail())
                .name(member.getName())
                .nickname(member.getNickname())
                .accessToken(tokenPair.getAccessToken())
                .refreshToken(tokenPair.getRefreshToken())
                .accessTokenExp(tokenPair.getAccessExpEpochSec())
                .refreshTokenExp(tokenPair.getRefreshExpEpochSec())
                .build();
    }

    /**
     * 토큰 갱신
     * Refresh Token을 이용하여 새로운 Access/Refresh Token 발급
     *
     * @param body 토큰 갱신 요청
     * @param req  HTTP 요청
     * @return 새로운 TokenPair
     */
    public TokenPair refresh(AuthDto.RefreshReq body, HttpServletRequest req) {
        String deviceId = CookieUtil.resolveDeviceId(req);
        TokenPair tokenPair = tokenService.rotateTokens(body.getRefreshToken(), deviceId);
        
        log.info("Token refresh successful: deviceId={}", deviceId);
        return tokenPair;
    }

    /**
     * 로그아웃
     * Access Token을 블랙리스트에 등록
     *
     * @param req HTTP 요청
     * @param res HTTP 응답
     */
    public void logout(HttpServletRequest req, HttpServletResponse res) {
        String accessToken = CookieUtil.resolveAccessToken(req);
        
        if (accessToken != null) {
            tokenService.logout(accessToken);
            log.info("Logout successful");
        }
    }

    @Transactional
    public void signup(AuthDto.SignupReq body) {

        // 1️⃣ 비밀번호 확인
        if (!body.getPassword().equals(body.getPasswordConfirm())) {
            throw new CustomException(ErrorCode.PASSWORD_MISMATCH);
        }

        // 2️⃣ 이메일 중복 검사
        if (memberRepository.existsByEmail(body.getEmail())) {
            throw new CustomException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        // 3️⃣ Member 생성
        Member member = Member.builder()
                .usinaId(body.getEmail())                 // 서버에서 생성
                .email(body.getEmail())
                .password(passwordEncoder.encode(body.getPassword()))
                .name(body.getName())
                .nickname(body.getNickname())
                .phone("000-0000-0000")                     // 임시 기본값 (NOT NULL 충족)
                .isAdmin(false)
                .build();

        memberRepository.save(member);
    }
}
