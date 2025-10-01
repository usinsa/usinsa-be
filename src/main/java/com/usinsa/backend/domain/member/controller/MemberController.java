package com.usinsa.backend.domain.member.controller;

import com.usinsa.backend.domain.member.dto.MemberResDto;
import com.usinsa.backend.domain.member.dto.SignupReqDto;
import com.usinsa.backend.domain.member.entity.Member;
import com.usinsa.backend.domain.member.repository.MemberRepository;
import com.usinsa.backend.domain.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api/v1/members")
public class MemberController {

    private final MemberService memberService;
    private final MemberRepository memberRepository;

    // 회원가입
    @PostMapping("/signup")
    public ResponseEntity<MemberResDto> signup(@Valid @RequestBody SignupReqDto req) {
        Member saved = memberService.signup(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(MemberResDto.from(saved));
    }

    // 유신아이디로 회원 조회 (예: 마이페이지 진입 전 간단 조회)
    @GetMapping("/{usinaId}")
    public ResponseEntity<MemberResDto> getByUsinaId(@PathVariable String usinaId) {
        Member m = memberService.loadByUsinaId(usinaId);
        return ResponseEntity.ok(MemberResDto.from(m));
    }
}
