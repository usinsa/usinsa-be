package com.usinsa.backend.domain.member.controller;

import com.usinsa.backend.domain.member.entity.Member;
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


    @PostMapping("/login")
    public ResponseEntity<Member> login(@Valid @RequestBody Member member) {
        Member loggedIn = memberService.login((member.getUsinaId()), member.getPassword());
        return ResponseEntity.ok(loggedIn);
    }
}
