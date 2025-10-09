package com.usinsa.backend.domain.member.service;

import com.usinsa.backend.domain.member.dto.LoginReqDto;
import com.usinsa.backend.domain.member.dto.MemberResDto;
import com.usinsa.backend.domain.member.dto.SignupReqDto;
import com.usinsa.backend.domain.member.entity.Member;
import com.usinsa.backend.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
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

    /*
     * 비밀번호를 비교할 때, DB에는 평문이 아닌 해시로 저장됨
     * 그렇기에 PasswordEncoder.matches(raw, encoded)를 사용함.
     * equals를 사용해 비밀번호 일치 여부를 확인하면 평문이랑 해시를 비교하는 상황이 발생함.
     */
    @Transactional(readOnly = true)
    public Member login(LoginReqDto loginReqDto) {
        Member member = memberRepository.findByUsinaId(loginReqDto.getUsinaId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유신아이디입니다."));

        if (!passwordEncoder.matches(loginReqDto.getPassword(), member.getPassword())) {
            throw new BadCredentialsException("비밀번호가 일치하지 않습니다.");
        }

        return member;
    }

}
