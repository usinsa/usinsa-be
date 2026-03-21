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
    public static final String GUEST_ID      = "guestId";

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
        String deviceId = request.getHeader("X-Device-Id");
        if (deviceId != null && !deviceId.isBlank()) return deviceId;
        String userAgent = Optional.ofNullable(request.getHeader("User-Agent")).orElse("unknown");
        return Integer.toHexString(Objects.hash(userAgent));
    }

    public static Optional<String> resolveGuestId(HttpServletRequest request) {
        return getCookie(request, GUEST_ID).map(Cookie::getValue);
    }

    public static void addCookie(HttpServletResponse response,
                                  String name, String value,
                                  int maxAgeSeconds, boolean secure, String domain) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(secure);
        cookie.setMaxAge(maxAgeSeconds);
        if (domain != null && !domain.isBlank()) {
            cookie.setDomain(domain);
        }
        response.addCookie(cookie);
    }

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

    public static void clearTokenCookies(HttpServletRequest request, HttpServletResponse response) {
        deleteCookie(request, response, ACCESS_TOKEN);
        deleteCookie(request, response, REFRESH_TOKEN);
    }
}
