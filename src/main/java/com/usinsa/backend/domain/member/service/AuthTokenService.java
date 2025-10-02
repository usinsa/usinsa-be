package com.usinsa.backend.domain.member.service;

import com.usinsa.backend.domain.member.entity.Member;
import com.usinsa.backend.standard.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthTokenService {

    @Value("${custom.jwt.secret-key}")
    private String keyString;

    @Value("${custom.jwt.expire-seconds}")
    private int expireSeconds;

    String genAcessToken(Member member) {
        return JwtUtil.createToken(
                keyString,
                expireSeconds,
                Map.of(
                        "id", member.getId(),
                        "usinaId", member.getUsinaId(),
                        "isAdmin", member.getIsAdmin()
                )
        );
    }
}
