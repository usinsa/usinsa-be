package com.usinsa.backend.domain.member.service;

import com.usinsa.backend.domain.member.entity.Member;
import com.usinsa.backend.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;

    @Transactional
    public Member create(Member member) {

        if (memberRepository.existsByEmail(member.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }
        if (memberRepository.existsByUsinaId(member.getUsinaId())) {
            throw new IllegalArgumentException("이미 사용 중인 유신아이디입니다.");
        }

        Member toSave = Member.builder()
                .usinaId(member.getUsinaId())
                .password(member.getPassword())
                .name(member.getName())
                .nickname(member.getNickname())
                .email(member.getEmail())
                .phone(member.getPhone())
                .profileImage(member.getProfileImage())
                .isAdmin(false)
                .build();

        return memberRepository.save(toSave);
    }

    @Transactional
    public Member login(String usinaId, String password) {
        Member member = memberRepository.findByUsinaId(usinaId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유신아이디입니다."));

        if (!member.getPassword().equals(password)) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        return member;
    }

}
