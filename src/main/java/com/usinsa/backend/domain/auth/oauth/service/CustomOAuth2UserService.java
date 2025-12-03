package com.usinsa.backend.domain.auth.oauth.service;

import com.usinsa.backend.domain.auth.oauth.OAuth2UserInfo;
import com.usinsa.backend.domain.auth.oauth.OAuth2UserInfoFactory;
import com.usinsa.backend.domain.member.entity.Member;
import com.usinsa.backend.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final MemberRepository memberRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = loadOAuth2User(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuth2UserInfo oAuth2UserInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(registrationId, oAuth2User);

        String providerId = oAuth2UserInfo.getId();
        String email = oAuth2UserInfo.getEmail();
        String provider = registrationId;

        Member member = memberRepository.findByEmailAndOauthProvider(email, provider)
                .orElseGet(() -> {
                    String usinaId = provider + "_" + providerId;
                    String randomPassword = UUID.randomUUID().toString();
                    return memberRepository.save(Member.builder()
                            .usinaId(usinaId)
                            .password(randomPassword)
                            .name(oAuth2UserInfo.getName())
                            .nickname(usinaId)
                            .email(email)
                            .phone("010-0000-0000") // Placeholder
                            .profileImage(oAuth2UserInfo.getImageUrl())
                            .oauthProvider(provider)
                            .oauthId(providerId)
                            .build());
                });

        return new PrincipalDetails(member, oAuth2User.getAttributes());
    }

    protected OAuth2User loadOAuth2User(OAuth2UserRequest userRequest) {
        return super.loadUser(userRequest);
    }
}
