package com.usinsa.backend.domain.member.controller;

import com.usinsa.backend.domain.member.dto.MemberResDto;
import com.usinsa.backend.domain.member.dto.SingupReqDto;
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
    public ResponseEntity<MemberResDto> signup(@Valid @RequestBody SingupReqDto singupReqDto) {
        MemberResDto created = memberService.singup(singupReqDto);
        return ResponseEntity.ok(created);
    }

    @PostMapping("/login")
    public ResponseEntity<Member> login(@Valid @RequestBody Member member) {
        Member loggedIn = memberService.login((member.getUsinaId()), member.getPassword());
        return ResponseEntity.ok(loggedIn);
    }
}
