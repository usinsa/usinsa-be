package com.usinsa.backend.domain.member.controller;

import com.usinsa.backend.domain.member.dto.LoginReqDto;
import com.usinsa.backend.domain.member.dto.LoginResDto;
import com.usinsa.backend.domain.member.dto.MemberResDto;
import com.usinsa.backend.domain.member.dto.SignupReqDto;
import com.usinsa.backend.domain.member.entity.Member;
import com.usinsa.backend.domain.member.service.AuthTokenService;
import com.usinsa.backend.domain.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api/v1/members")
public class MemberController {

    private final MemberService memberService;
    private final AuthTokenService authTokenService;

    @PostMapping("/signup")
    public ResponseEntity<MemberResDto> signup(@Valid @RequestBody SignupReqDto signupReqDto) {
        MemberResDto memberResDto = memberService.signup(signupReqDto);
        return ResponseEntity.ok(memberResDto);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResDto> login(@Valid @RequestBody LoginReqDto loginReqDto) {
        Member member = memberService.login(loginReqDto);
        String accessToken = authTokenService.genAcessToken(member);

        LoginResDto loginResDto = LoginResDto.builder()
                .memberId(member.getId())
                .email(member.getEmail())
                .name(member.getName())
                .nickname(member.getNickname())
                .accessToken(accessToken)
                .build();

        return ResponseEntity.ok(loginResDto);
    }
}
