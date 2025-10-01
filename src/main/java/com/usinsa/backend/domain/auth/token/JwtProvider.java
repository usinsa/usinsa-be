package com.usinsa.backend.domain.auth.token;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtProvider {

    private final TokenProperties props;

    private Key accessKey;
    private Key refreshKey;

    @PostConstruct
    void init() {
        this.accessKey  = Keys.hmacShaKeyFor(props.getAccess().getSecret().getBytes(StandardCharsets.UTF_8));
        this.refreshKey = Keys.hmacShaKeyFor(props.getRefresh().getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String createAccess(String subjectEmail, Collection<? extends GrantedAuthority> roles) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setIssuer(props.getIssuer())
                .setSubject(subjectEmail)
                .claim("roles", roles.stream().map(GrantedAuthority::getAuthority).toList())
                .claim("type", TokenType.ACCESS.name())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(props.getAccess().getExpiration())))
                .signWith(accessKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public String createRefresh(String subjectEmail) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setIssuer(props.getIssuer())
                .setSubject(subjectEmail)
                .claim("type", TokenType.REFRESH.name())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(props.getRefresh().getExpiration())))
                .signWith(refreshKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public Jws<Claims> parseAccess(String jwt) {
        return Jwts.parserBuilder()
                .setSigningKey(accessKey)
                .build()
                .parseClaimsJws(jwt);
    }

    public Jws<Claims> parseRefresh(String jwt) {
        return Jwts.parserBuilder()
                .setSigningKey(refreshKey)
                .build()
                .parseClaimsJws(jwt);
    }
}