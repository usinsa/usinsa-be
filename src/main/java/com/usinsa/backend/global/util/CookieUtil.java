package com.usinsa.backend.global.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

import java.util.Objects;
import java.util.Optional;

public class CookieUtil {

    public static final String ACCESS_TOKEN  = "accessToken";
    public static final String REFRESH_TOKEN = "refreshToken";
    public static final String GUEST_ID      = "guestId";
    public static final String DEVICE_ID     = "deviceId";

    public static Optional<Cookie> getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return Optional.empty();
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) return Optional.of(cookie);
        }
        return Optional.empty();
    }

    public static String resolveAccessToken(HttpServletRequest request) {
        return getCookie(request, ACCESS_TOKEN)
                .map(Cookie::getValue)
                .orElseGet(() -> {
                    String header = request.getHeader(HttpHeaders.AUTHORIZATION);
                    return (header != null && header.startsWith("Bearer "))
                            ? header.substring(7) : null;
                });
    }

        public static String resolveDeviceId(HttpServletRequest request) {
        // 쿠키에서 먼저 조회 — 없으면 X-Device-Id 헤더 — 없으면 User-Agent 해시
        return getCookie(request, DEVICE_ID)
                .map(Cookie::getValue)
                .orElseGet(() -> {
                    String header = request.getHeader("X-Device-Id");
                    if (header != null && !header.isBlank()) return header;
                    String userAgent = Optional.ofNullable(request.getHeader("User-Agent")).orElse("unknown");
                    return Integer.toHexString(Objects.hash(userAgent));
                });
    }

    /** 로그인 시 deviceId를 쿠키로 발급 — 이후 요청에서 동일한 deviceId 보장 */
    public static void addDeviceIdCookie(HttpServletResponse response,
                                          String deviceId, boolean secure, String domain) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(DEVICE_ID, deviceId)
                .path("/")
                .httpOnly(true)
                .secure(secure)
                .maxAge(60 * 60 * 24 * 365) // 1년
                .sameSite(secure ? "None" : "Lax");
        if (domain != null && !domain.isBlank()) builder.domain(domain);
        response.addHeader(HttpHeaders.SET_COOKIE, builder.build().toString());
    }

    public static Optional<String> resolveGuestId(HttpServletRequest request) {
        return getCookie(request, GUEST_ID).map(Cookie::getValue);
    }

    /**
     * ResponseCookie 활용 — SameSite 속성 지원
     * secure=true(운영): SameSite=None (크로스 서브도메인 전송)
     * secure=false(개발): SameSite=Lax
     */
    public static void addCookie(HttpServletResponse response,
                                  String name, String value,
                                  int maxAgeSeconds, boolean secure, String domain) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
                .path("/")
                .httpOnly(true)
                .secure(secure)
                .maxAge(maxAgeSeconds)
                .sameSite(secure ? "None" : "Lax");

        if (domain != null && !domain.isBlank()) {
            builder.domain(domain);
        }

        response.addHeader(HttpHeaders.SET_COOKIE, builder.build().toString());
    }

    public static void deleteCookie(HttpServletRequest request,
                                     HttpServletResponse response, String name,
                                     boolean secure, String domain) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, "")
                .path("/")
                .httpOnly(true)
                .secure(secure)
                .maxAge(0)
                .sameSite(secure ? "None" : "Lax");

        if (domain != null && !domain.isBlank()) {
            builder.domain(domain);
        }

        response.addHeader(HttpHeaders.SET_COOKIE, builder.build().toString());
    }

    public static void clearTokenCookies(HttpServletRequest request, HttpServletResponse response,
                                          boolean secure, String domain) {
        deleteCookie(request, response, ACCESS_TOKEN, secure, domain);
        deleteCookie(request, response, REFRESH_TOKEN, secure, domain);
    }
}
