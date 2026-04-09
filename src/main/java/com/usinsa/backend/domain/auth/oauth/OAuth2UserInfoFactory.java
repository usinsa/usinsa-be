package com.usinsa.backend.domain.auth.oauth;

import com.usinsa.backend.domain.auth.oauth.provider.GoogleUserInfo;
import com.usinsa.backend.domain.auth.oauth.provider.KakaoUserInfo;
import com.usinsa.backend.domain.auth.oauth.provider.NaverUserInfo;
import com.usinsa.backend.global.exception.CustomException;
import com.usinsa.backend.global.exception.ErrorCode;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Map;

public class OAuth2UserInfoFactory {

    // Spring Security OAuth2 자동 처리 흐름용 (OAuth2User 기반)
    public static OAuth2UserInfo getOAuth2UserInfo(String registrationId, OAuth2User oAuth2User) {
        return getOAuth2UserInfo(registrationId, oAuth2User.getAttributes());
    }

    // Authorization Code 수동 처리 흐름용 (Map 기반)
    public static OAuth2UserInfo getOAuth2UserInfo(String registrationId, Map<String, Object> attributes) {
        return switch (registrationId.toLowerCase()) {
            case "google" -> new GoogleUserInfo(attributes);
            case "naver" -> new NaverUserInfo(attributes);
            case "kakao" -> new KakaoUserInfo(attributes);
            default -> throw new CustomException(ErrorCode.INVALID_PROVIDER_TYPE);
        };
    }
}
