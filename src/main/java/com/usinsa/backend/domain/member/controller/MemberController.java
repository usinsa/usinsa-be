package com.usinsa.backend.domain.member.controller;

import com.usinsa.backend.domain.member.dto.AuthDto;
import com.usinsa.backend.domain.member.dto.MemberDto;
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
    public ResponseEntity<MemberDto.Response> signup(@Valid @RequestBody AuthDto.SignupReq signupReqDto) {
        MemberDto.Response memberResDto = memberService.signup(signupReqDto);
        return ResponseEntity.ok(memberResDto);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthDto.LoginRes> login(@Valid @RequestBody AuthDto.LoginReq loginReqDto) {
        return ResponseEntity.ok(memberService.login(loginReqDto));
    }
}
