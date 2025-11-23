package com.usinsa.backend.domain.member.service;

import com.usinsa.backend.domain.member.dto.LoginReqDto;
import com.usinsa.backend.domain.member.dto.MemberResDto;
import com.usinsa.backend.domain.member.dto.SignupReqDto;
import com.usinsa.backend.domain.member.entity.Member;
import com.usinsa.backend.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @InjectMocks
    private MemberService memberService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private Member testMember;
    private SignupReqDto signupReqDto;
    private LoginReqDto loginReqDto;

    @BeforeEach
    void setUp() {
        testMember = Member.builder()
                .id(1L)
                .usinaId("testuser")
                .password("encodedPassword")
                .name("테스트유저")
                .nickname("테스터")
                .email("test@test.com")
                .phone("010-1234-5678")
                .profileImage("profile.jpg")
                .isAdmin(false)
                .build();

        signupReqDto = new SignupReqDto(
                "testuser",
                "password123",
                "테스트유저",
                "테스터",
                "test@test.com",
                "010-1234-5678",
                "profile.jpg"
        );

        loginReqDto = new LoginReqDto("testuser", "password123");
    }

    @Nested
    @DisplayName("회원가입 테스트")
    class SignupTest {

        @Test
        @DisplayName("정상적으로 회원가입에 성공한다")
        void signup_Success() {
            // given
            given(memberRepository.existsByEmail(anyString())).willReturn(false);
            given(memberRepository.existsByUsinaId(anyString())).willReturn(false);
            given(passwordEncoder.encode(anyString())).willReturn("encodedPassword");
            given(memberRepository.save(any(Member.class))).willReturn(testMember);

            // when
            MemberResDto result = memberService.signup(signupReqDto);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getUsinaId()).isEqualTo("testuser");
            assertThat(result.getEmail()).isEqualTo("test@test.com");
            assertThat(result.getName()).isEqualTo("테스트유저");
            verify(memberRepository).save(any(Member.class));
        }

        @Test
        @DisplayName("이미 사용 중인 이메일이면 회원가입에 실패한다")
        void signup_DuplicateEmail_Fail() {
            // given
            given(memberRepository.existsByEmail(anyString())).willReturn(true);

            // when & then
            assertThatThrownBy(() -> memberService.signup(signupReqDto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("이미 사용 중인 이메일입니다.");
        }

        @Test
        @DisplayName("이미 사용 중인 유신아이디면 회원가입에 실패한다")
        void signup_DuplicateUsinaId_Fail() {
            // given
            given(memberRepository.existsByEmail(anyString())).willReturn(false);
            given(memberRepository.existsByUsinaId(anyString())).willReturn(true);

            // when & then
            assertThatThrownBy(() -> memberService.signup(signupReqDto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("이미 사용 중인 유신아이디입니다.");
        }
    }

    @Nested
    @DisplayName("로그인 테스트")
    class LoginTest {

        @Test
        @DisplayName("정상적으로 로그인에 성공한다")
        void login_Success() {
            // given
            given(memberRepository.findByUsinaId(anyString())).willReturn(Optional.of(testMember));
            given(passwordEncoder.matches(anyString(), anyString())).willReturn(true);

            // when
            Member result = memberService.login(loginReqDto);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getUsinaId()).isEqualTo("testuser");
        }

        @Test
        @DisplayName("존재하지 않는 아이디면 로그인에 실패한다")
        void login_UserNotFound_Fail() {
            // given
            given(memberRepository.findByUsinaId(anyString())).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> memberService.login(loginReqDto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("존재하지 않는 유신아이디입니다.");
        }

        @Test
        @DisplayName("비밀번호가 일치하지 않으면 로그인에 실패한다")
        void login_WrongPassword_Fail() {
            // given
            given(memberRepository.findByUsinaId(anyString())).willReturn(Optional.of(testMember));
            given(passwordEncoder.matches(anyString(), anyString())).willReturn(false);

            // when & then
            assertThatThrownBy(() -> memberService.login(loginReqDto))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessage("비밀번호가 일치하지 않습니다.");
        }
    }
}
