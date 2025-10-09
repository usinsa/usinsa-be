package com.usinsa.backend.domain.member.service;

import com.usinsa.backend.domain.member.dto.MemberResDto;
import com.usinsa.backend.domain.member.dto.SignupReqDto;
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
    public MemberResDto singup(SignupReqDto signupReqDto) {
        if (memberRepository.existsByEmail(signupReqDto.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }
        if (memberRepository.existsByUsinaId(signupReqDto.getUsinaId())) {
            throw new IllegalArgumentException("이미 사용 중인 유신아이디입니다.");
        }

        String hash = passwordEncoder.encode(signupReqDto.getPassword());

        Member toSave = memberRepository.save(
                Member.builder()
                        .usinaId(signupReqDto.getUsinaId())
                        .password(hash) // 해시된 비밀번호만 저장
                        .name(signupReqDto.getName())
                        .nickname(signupReqDto.getNickname())
                        .email(signupReqDto.getEmail().toLowerCase()) // 정규화 가능
                        .phone(signupReqDto.getPhone())
                        .profileImage(signupReqDto.getProfileImage())
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
