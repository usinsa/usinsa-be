package com.usinsa.backend.global.security.token;

import com.usinsa.backend.global.security.token.store.RefreshTokenStore;
import com.usinsa.backend.global.security.token.store.TokenBlacklist;
import com.usinsa.backend.standard.util.Ut;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AuthTokenService {

    private final JwtProps props;
    private final RefreshTokenStore refreshStore;
    private final TokenBlacklist blacklist;

    public TokenPair issue(Long memberId, String email, List<String> roles, String deviceId) {
        Instant now = Instant.now();
        String jtiAccess = UUID.randomUUID().toString();
        String jtiRefresh = UUID.randomUUID().toString();

        Map<String,Object> access = new HashMap<>();
        access.put("uid", memberId);
        access.put("rol", roles);
        access.put("jti", jtiAccess);
        access.put("typ", "access");
        String accessToken = Ut.Jwt.createToken(props.getSecret(), props.getAccessExpSeconds(), access);
        long accessExp = now.plusSeconds(props.getAccessExpSeconds()).getEpochSecond();

        Map<String,Object> refresh = new HashMap<>();
        refresh.put("uid", memberId);
        refresh.put("jti", jtiRefresh);
        refresh.put("dev", deviceId);
        refresh.put("typ", "refresh");
        String refreshToken = Ut.Jwt.createToken(props.getSecret(), props.getRefreshExpSeconds(), refresh);
        Instant refreshExp = now.plusSeconds(props.getRefreshExpSeconds());

        refreshStore.save(TokenMeta.builder()
                .jti(jtiRefresh).memberId(memberId).email(email).roles(roles)
                .deviceId(deviceId).expiresAt(refreshExp).build());

        return TokenPair.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .accessExpEpochSec(accessExp)
                .refreshExpEpochSec(refreshExp.getEpochSecond())
                .build();
    }

    public TokenPair rotate(String refreshToken, String deviceId) {
        Claims c = Ut.Jwt.parse(props.getSecret(), refreshToken);
        if (!"refresh".equals(c.get("typ"))) throw new IllegalStateException("TOKEN_INVALID");
        Long uid = ((Number) c.get("uid")).longValue();
        String jti = (String) c.get("jti");

        var latestOpt = refreshStore.find(uid, deviceId);
        if (latestOpt.isEmpty()) throw new IllegalStateException("TOKEN_REVOKED");
        var latest = latestOpt.get();
        if (!Objects.equals(latest.getJti(), jti)) throw new IllegalStateException("TOKEN_REUSED");

        return issue(uid, latest.getEmail(), latest.getRoles(), deviceId);
    }

    public void logout(String accessToken) {
        Claims c = Ut.Jwt.parse(props.getSecret(), accessToken);
        if (!"access".equals(c.get("typ"))) return;
        String jti = (String) c.get("jti");
        Instant exp = c.getExpiration().toInstant();
        blacklist.blacklist(jti, exp);
    }

    public boolean isBlacklisted(String accessToken) {
        Claims c = Ut.Jwt.parse(props.getSecret(), accessToken);
        String jti = (String) c.get("jti");
        return blacklist.isBlacklisted(jti);
    }

    public String resolveAccessToken(HttpServletRequest req) {
        String h = req.getHeader(HttpHeaders.AUTHORIZATION);
        if (h != null && h.startsWith("Bearer ")) return h.substring(7);
        return null;
    }

    public String resolveDeviceId(HttpServletRequest req) {
        String d = req.getHeader("X-Device-Id");
        if (d != null && !d.isBlank()) return d;
        String ua = Optional.ofNullable(req.getHeader("User-Agent")).orElse("na");
        return Integer.toHexString(Objects.hash(ua));
    }

    public JwtProps getProps() { return props; } // 필터에서 필요 시 사용
}
