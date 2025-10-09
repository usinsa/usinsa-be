package com.usinsa.backend.domain.member.controller;

import com.usinsa.backend.domain.member.dto.LoginReqDto;
import com.usinsa.backend.domain.member.dto.MemberResDto;
import com.usinsa.backend.domain.member.dto.SignupReqDto;
import com.usinsa.backend.domain.member.entity.Member;
import com.usinsa.backend.domain.member.repository.MemberRepository;
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
    private final MemberRepository memberRepository;

    @PostMapping("/signup")
    public ResponseEntity<MemberResDto> signup(@Valid @RequestBody SignupReqDto signupReqDto) {
        MemberResDto created = memberService.singup(signupReqDto);
        return ResponseEntity.ok(created);
    }

    @PostMapping("/login")
    public ResponseEntity<Member> login(@Valid @RequestBody LoginReqDto loginReqDto) {
        Member loggedIn = memberService.login(loginReqDto);
        return ResponseEntity.ok(loggedIn);
    }
}
