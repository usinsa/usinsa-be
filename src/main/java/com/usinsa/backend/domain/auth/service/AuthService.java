package com.usinsa.backend.domain.auth.service;

import com.usinsa.backend.domain.auth.token.JwtProvider;
import com.usinsa.backend.domain.auth.token.TokenProperties;
import com.usinsa.backend.domain.auth.token.TokenStore;
import com.usinsa.backend.domain.member.entity.Member;
import com.usinsa.backend.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Date;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwt;
    private final TokenStore tokenStore;
    private final MemberRepository memberRepository;
    private final TokenProperties props;

    /**
     * 로그인: 자격 검증 → 토큰 2종 발급 → refresh 저장
     * 예외:
     *  - 400: 필수값 누락
     *  - 401: 인증 실패(아이디/비번 불일치)
     *  - 404: 회원 없음
     */
    public TokenResponse login(String usinaIdOrEmail, String rawPassword) {
        final String idOrEmail = normalize(usinaIdOrEmail);
        if (isBlank(idOrEmail) || isBlank(rawPassword)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "아이디(또는 이메일)와 비밀번호는 필수입니다.");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(idOrEmail, rawPassword)
            );
        } catch (BadCredentialsException ex) {
            // 인증 실패는 401로 통일 (보안상 구체 사유 노출 지양)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다.");
        } catch (Exception ex) {
            // 기타 인증 오류도 401
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증에 실패했습니다.");
        }

        // UDS 전략에 맞춰 식별: usinaId 우선 → email 보조
        Member m = memberRepository.findByUsinaId(idOrEmail)
                .orElseGet(() -> memberRepository.findByEmail(idOrEmail.toLowerCase())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "회원 없음")));

        String access  = jwt.createAccess(m.getEmail(), m.authorities());
        String refresh = jwt.createRefresh(m.getEmail());
        tokenStore.storeRefresh(m.getEmail(), refresh, props.getRefresh().getExpiration());

        return new TokenResponse(
                access, refresh,
                props.getAccess().getExpiration().toSeconds(),
                props.getRefresh().getExpiration().toSeconds()
        );
    }

    /**
     * 재발급: 저장된 refresh와 완전 일치해야 함 → 회전
     * 예외:
     *  - 400: 필수값 누락
     *  - 401: refresh 불일치/없음
     *  - 404: 회원 없음
     */
    public TokenResponse refresh(String email, String refreshToken) {
        final String normalizedEmail = normalizeEmail(email);
        if (isBlank(normalizedEmail) || isBlank(refreshToken)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이메일과 리프레시 토큰은 필수입니다.");
        }

        String stored = tokenStore.getRefresh(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "리프레시 토큰 없음"));

        if (!stored.equals(refreshToken)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "리프레시 토큰 불일치");
        }

        Member m = memberRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "회원 없음"));

        String newAccess  = jwt.createAccess(m.getEmail(), m.authorities());
        String newRefresh = jwt.createRefresh(m.getEmail());
        tokenStore.rotateRefresh(normalizedEmail, newRefresh, props.getRefresh().getExpiration());

        return new TokenResponse(
                newAccess, newRefresh,
                props.getAccess().getExpiration().toSeconds(),
                props.getRefresh().getExpiration().toSeconds()
        );
    }

    /**
     * 로그아웃: access 남은 TTL만큼 블랙리스트, refresh 무효화
     * 예외:
     *  - 400: 토큰 파싱 불가/형식 오류
     */
    public void logout(String accessToken, String email) {
        if (isBlank(accessToken) || isBlank(email)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "액세스 토큰과 이메일은 필수입니다.");
        }

        Date exp;
        try {
            exp = jwt.parseAccess(accessToken).getBody().getExpiration();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "유효하지 않은 액세스 토큰입니다.");
        }

        long sec = Math.max(0, (exp.getTime() - System.currentTimeMillis()) / 1000);
        tokenStore.blacklistAccess(accessToken, Duration.ofSeconds(sec));
        tokenStore.rotateRefresh(normalizeEmail(email), "", Duration.ofSeconds(1)); // invalidate
    }

    // ===== 내부 유틸 =====
    private static String normalize(String s) { return s == null ? null : s.trim(); }
    private static String normalizeEmail(String s) { return s == null ? null : s.trim().toLowerCase(); }
    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
}
