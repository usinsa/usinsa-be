package com.usinsa.backend.domain.auth.oauth.service;

import com.usinsa.backend.domain.member.entity.Member;
import com.usinsa.backend.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomOAuth2UserServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private CustomOAuth2UserService customOAuth2UserService;

    private OAuth2UserRequest oAuth2UserRequest;
    private OAuth2User oAuth2User;
    private Member member;

    @BeforeEach
    void setUp() {
        ClientRegistration clientRegistration = ClientRegistration.withRegistrationId("google")
                .clientId("test-client-id")
                .clientSecret("test-client-secret")
                .authorizationGrantType(org.springframework.security.oauth2.core.AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("profile", "email")
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .tokenUri("https://www.googleapis.com/oauth2/v4/token")
                .userInfoUri("https://www.googleapis.com/oauth2/v3/userinfo")
                .userNameAttributeName("sub")
                .clientName("Google")
                .build();

        oAuth2UserRequest = new OAuth2UserRequest(clientRegistration, new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "test-token", null, null));

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("sub", "123456789");
        attributes.put("name", "Test User");
        attributes.put("email", "test@example.com");
        attributes.put("picture", "https://example.com/test.jpg");
        oAuth2User = new DefaultOAuth2User(null, attributes, "sub");

        member = Member.builder()
                .id(1L)
                .usinaId("google_123456789")
                .password(UUID.randomUUID().toString())
                .name("Test User")
                .nickname("google_123456789")
                .email("test@example.com")
                .phone("010-0000-0000")
                .profileImage("https://example.com/test.jpg")
                .oauthProvider("google")
                .oauthId("123456789")
                .build();

        customOAuth2UserService = new CustomOAuth2UserService(memberRepository);
    }

    @Test
    @DisplayName("기존 회원이 OAuth2로 로그인 시 회원 정보를 반환한다")
    void loadUser_existingUser() {
        // given
        CustomOAuth2UserService spy = org.mockito.Mockito.spy(customOAuth2UserService);
        when(memberRepository.findByEmailAndOauthProvider("test@example.com", "google"))
                .thenReturn(Optional.of(member));
        doReturn(oAuth2User).when(spy).loadOAuth2User(any());


        // when
        PrincipalDetails principalDetails = (PrincipalDetails) spy.loadUser(oAuth2UserRequest);

        // then
        assertThat(principalDetails.getMember()).isEqualTo(member);
    }

    @Test
    @DisplayName("신규 회원이 OAuth2로 로그인 시 회원 가입 후 회원 정보를 반환한다")
    void loadUser_newUser() {
        // given
        CustomOAuth2UserService spy = org.mockito.Mockito.spy(customOAuth2UserService);
        when(memberRepository.findByEmailAndOauthProvider("test@example.com", "google"))
                .thenReturn(Optional.empty());
        when(memberRepository.save(any(Member.class))).thenReturn(member);
        doReturn(oAuth2User).when(spy).loadOAuth2User(any());

        // when
        PrincipalDetails principalDetails = (PrincipalDetails) spy.loadUser(oAuth2UserRequest);

        // then
        assertThat(principalDetails.getMember().getEmail()).isEqualTo("test@example.com");
        assertThat(principalDetails.getMember().getOauthProvider()).isEqualTo("google");
    }
}
