package com.usinsa.backend.domain.member.service;

import com.usinsa.backend.domain.member.entity.Member;
import com.usinsa.backend.standard.util.Ut;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthTokenService {

    @Value("${custom.jwt.secret-key}")
    private String keyString;

    @Value("${custom.jwt.expire-seconds}")
    private int expireSeconds;

    public String genAccessToken(Member member) {

        // secretKey를 -> Key 객체로 생성
        Key secretKey = Keys.hmacShaKeyFor(keyString.getBytes(StandardCharsets.UTF_8));

        return Ut.Jwt.createToken(
                secretKey,
                expireSeconds,
                Map.of(
                        "id", member.getId(),
                        "usinaId", member.getUsinaId(),
                        "isAdmin", member.getIsAdmin()
                )
        );
    }
}
