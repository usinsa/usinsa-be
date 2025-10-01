package com.usinsa.backend.domain.member.service;

import com.usinsa.backend.domain.member.dto.MemberResDto;
import com.usinsa.backend.domain.member.dto.SingupReqDto;
import com.usinsa.backend.domain.member.entity.Member;
import com.usinsa.backend.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public MemberResDto singup(SingupReqDto singupReqDto) {
        if (memberRepository.existsByEmail(singupReqDto.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }
        if (memberRepository.existsByUsinaId(singupReqDto.getUsinaId())) {
            throw new IllegalArgumentException("이미 사용 중인 유신아이디입니다.");
        }

        String hash = passwordEncoder.encode(singupReqDto.getPassword());

        Member toSave = memberRepository.save(
                Member.builder()
                        .usinaId(singupReqDto.getUsinaId())
                        .password(hash) // 해시된 비밀번호만 저장
                        .name(singupReqDto.getName())
                        .nickname(singupReqDto.getNickname())
                        .email(singupReqDto.getEmail().toLowerCase()) // 정규화 가능
                        .phone(singupReqDto.getPhone())
                        .profileImage(singupReqDto.getProfileImage())
                        .build() // isAdmin은 기본값 false
        );
        return MemberResDto.from(toSave);
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
