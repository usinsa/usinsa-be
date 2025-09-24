package com.usinsa.backend.domain.member.service;

import com.usinsa.backend.domain.member.entity.Member;
import com.usinsa.backend.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@SpringBootTest
@Transactional
public class MemberServiceTest {

    @Autowired
    MemberService memberService;

    @Autowired
    MemberRepository memberRepository;

    @TestConfiguration
    static class TestConfig {
        @Bean
        PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }
    }

    @Test
    @DisplayName("멤버 생성")
    void createMember() {
        Member member = Member.builder()
                .usinaId("kozae")
                .password("plain") // TODO : 암호화 필요
                .name("고재현")
                .nickname("ko")
                .email("test@test.com")
                .phone("010-1111-2222")
                .isAdmin(false)
                .build();

        Member saved = memberService.create(member);

        assertThat(saved.getMemberId()).isNotNull();
        assertThat(saved.getUsinaId()).isEqualTo("kozae");
        assertThat(saved.getEmail()).isEqualTo("test@test.com");
    }

    @Test
    @DisplayName("이메일 중복 시 예외가 발생한다")
    void create_duplicateEmail() {
        // given
        Member m1 = Member.builder()
                .usinaId("id1")
                .password("pw")
                .name("n1")
                .nickname("nick1")
                .email("dup@test.com")
                .phone("010-1")
                .isAdmin(false)
                .build();
        memberRepository.save(m1);

        Member m2 = Member.builder()
                .usinaId("id2")
                .password("pw")
                .name("n2")
                .nickname("nick2")
                .email("dup@test.com") // same
                .phone("010-2")
                .isAdmin(false)
                .build();

        // when & then
        assertThatThrownBy(() -> memberService.create(m2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이메일");
    }
}
