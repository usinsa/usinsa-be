package com.usinsa.backend.domain.auth.service;

import com.usinsa.backend.domain.member.entity.Member;
import com.usinsa.backend.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // username으로 email 먼저 시도 → 실패 시 usinaId 시도
        Member m = memberRepository.findByEmail(toEmail(username))
                .orElseGet(() -> memberRepository.findByUsinaId(username)
                        .orElseThrow(() -> new UsernameNotFoundException("Member not found")));


        // Security의 principal username은 이메일로 통일(토큰 subject와 일관)
        return new User(
                m.getEmail(),
                m.getPassword(),
                m.authorities()
        );
    }

    private String toEmail(String s) { return s == null ? null : s.trim().toLowerCase(); }
}
