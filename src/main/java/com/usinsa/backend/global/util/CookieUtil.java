package com.usinsa.backend.global.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;

import java.util.Objects;
import java.util.Optional;

public class CookieUtil {

    public static final String ACCESS_TOKEN  = "accessToken";
    public static final String REFRESH_TOKEN = "refreshToken";
    public static final String GUEST_ID      = "guestId";     // 비회원 장바구니 식별자

    // ── 조회 ──────────────────────────────────────────────────────────

    public static Optional<Cookie> getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return Optional.empty();
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) return Optional.of(cookie);
        }
        return Optional.empty();
    }

    public static String resolveAccessToken(HttpServletRequest request) {
        // 쿠키 우선, 없으면 Authorization 헤더
        return getCookie(request, ACCESS_TOKEN)
                .map(Cookie::getValue)
                .orElseGet(() -> {
                    String header = request.getHeader(HttpHeaders.AUTHORIZATION);
                    return (header != null && header.startsWith("Bearer "))
                            ? header.substring(7) : null;
                });
    }

    public static String resolveDeviceId(HttpServletRequest request) {
        String deviceId = request.getHeader("X-Device-Id");
        if (deviceId != null && !deviceId.isBlank()) return deviceId;
        String userAgent = Optional.ofNullable(request.getHeader("User-Agent")).orElse("unknown");
        return Integer.toHexString(Objects.hash(userAgent));
    }

    /** 비회원 장바구니 식별자: guestId 쿠키에서 추출 */
    public static Optional<String> resolveGuestId(HttpServletRequest request) {
        return getCookie(request, GUEST_ID).map(Cookie::getValue);
    }

    // ── 발급 ──────────────────────────────────────────────────────────

    /**
     * HttpOnly 쿠키 추가
     *
     * @param secure true = HTTPS 전용 (운영), false = HTTP 허용 (개발)
     */
    public static void addCookie(HttpServletResponse response,
                                  String name, String value,
                                  int maxAgeSeconds, boolean secure) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(secure);
        cookie.setMaxAge(maxAgeSeconds);
        response.addCookie(cookie);
    }

    /** 개발 편의용 오버로드 (secure=false) */
    public static void addCookie(HttpServletResponse response,
                                  String name, String value, int maxAgeSeconds) {
        addCookie(response, name, value, maxAgeSeconds, false);
    }

    // ── 삭제 ──────────────────────────────────────────────────────────

    public static void deleteCookie(HttpServletRequest request,
                                     HttpServletResponse response, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return;
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                cookie.setValue("");
                cookie.setPath("/");
                cookie.setMaxAge(0);
                response.addCookie(cookie);
            }
        }
    }

    /** Access/Refresh 토큰 쿠키 일괄 삭제 */
    public static void clearTokenCookies(HttpServletRequest request, HttpServletResponse response) {
        deleteCookie(request, response, ACCESS_TOKEN);
        deleteCookie(request, response, REFRESH_TOKEN);
    }
}
