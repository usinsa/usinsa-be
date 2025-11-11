package com.usinsa.backend.domain.auth.service;

import com.usinsa.backend.domain.auth.dto.AuthDto;
import com.usinsa.backend.domain.member.entity.Member;
import com.usinsa.backend.domain.member.repository.MemberRepository;
import com.usinsa.backend.global.security.token.AuthTokenService;
import com.usinsa.backend.global.security.token.TokenPair;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthTokenService tokenService;

    public AuthDto.LoginRes login(AuthDto.LoginReq body, HttpServletRequest req, HttpServletResponse res) {
        Member m = memberRepository.findByEmail(body.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("INVALID_LOGIN_INFO"));
        if (!passwordEncoder.matches(body.getPassword(), m.getPassword())) {
            throw new IllegalArgumentException("INVALID_LOGIN_INFO");
        }

        String deviceId = tokenService.resolveDeviceId(req);
        // 역할 로딩 방식은 프로젝트 정책에 맞게
        List<String> roles = List.of("ROLE_USER");

        TokenPair pair = tokenService.issue(m.getId(), m.getEmail(), roles, deviceId);

        // 쿠키를 쓰고 싶다면 여기서 set-Cookie 처리 전담 클래스를 두고 설정해도 됨(옵션)
        return AuthDto.LoginRes.builder()
                .memberId(m.getId())
                .email(m.getEmail())
                .name(m.getName())
                .nickname(m.getNickname())
                .accessToken(pair.getAccessToken())
                .refreshToken(pair.getRefreshToken())
                .accessTokenExp(pair.getAccessExpEpochSec())
                .refreshTokenExp(pair.getRefreshExpEpochSec())
                .build();
    }

    public TokenPair refresh(AuthDto.RefreshReq body, HttpServletRequest req) {
        String deviceId = tokenService.resolveDeviceId(req);
        return tokenService.rotate(body.getRefreshToken(), deviceId);
    }

    public void logout(HttpServletRequest req, HttpServletResponse res) {
        String access = tokenService.resolveAccessToken(req);
        if (access != null) tokenService.logout(access);
        // 쿠키 삭제가 필요하면 전담 CookieManager로 분리하여 여기서 호출
    }
}
