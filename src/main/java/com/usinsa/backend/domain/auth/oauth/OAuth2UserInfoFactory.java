package com.usinsa.backend.domain.auth.oauth;

import com.usinsa.backend.domain.auth.oauth.provider.GoogleUserInfo;
import com.usinsa.backend.domain.auth.oauth.provider.KakaoUserInfo;
import com.usinsa.backend.domain.auth.oauth.provider.NaverUserInfo;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Map;

public class OAuth2UserInfoFactory {
    public static OAuth2UserInfo getOAuth2UserInfo(String registrationId, OAuth2User oAuth2User) {
        Map<String, Object> attributes = oAuth2User.getAttributes();
        return switch (registrationId.toLowerCase()) {
            case "google" -> new GoogleUserInfo(attributes);
            case "naver" -> new NaverUserInfo(attributes);
            case "kakao" -> new KakaoUserInfo(attributes);
            default -> throw new IllegalArgumentException("Invalid Provider Type.");
        };
    }
}
