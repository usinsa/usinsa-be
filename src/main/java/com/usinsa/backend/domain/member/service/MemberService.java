package com.usinsa.backend.domain.member.service;

import com.usinsa.backend.domain.member.dto.SignupReqDto;
import com.usinsa.backend.domain.member.entity.Member;
import com.usinsa.backend.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Member signup(SignupReqDto req) {
        final String email   = normalizeEmail(req.getEmail());
        final String usinaId = normalizeUsinaId(req.getUsinaId());
        final String phone   = normalizePhone(req.getPhone());

        if (email == null || email.isBlank())   throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이메일은 필수입니다.");
        if (usinaId == null || usinaId.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "유신아이디는 필수입니다.");
        if (req.getPassword() == null || req.getPassword().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "비밀번호는 필수입니다.");
        }

        if (memberRepository.existsByEmail(email)) {
            // 409 CONFLICT: 리소스 충돌(중복 가입)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다.");
        }
        if (memberRepository.existsByUsinaId(usinaId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용 중인 유신아이디입니다.");
        }

        String hash = passwordEncoder.encode(req.getPassword());

        Member saved = memberRepository.save(
                Member.createNew(usinaId, hash, req.getName(), req.getNickname(), email, phone, req.getProfileImage())
        );
        return saved;
    }

    @Transactional(readOnly = true)
    public Member loadByUsinaId(String usinaId) {
        String normalized = normalizeUsinaId(usinaId);
        if (normalized == null || normalized.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "유신아이디는 필수입니다.");
        }

        return memberRepository.findByUsinaId(normalized)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "회원이 존재하지 않습니다."));
    }

    // ===== 정규화 유틸 =====
    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
    private String normalizeUsinaId(String id) {
        return id == null ? null : id.trim();
    }
    private String normalizePhone(String phone) {
        return phone == null ? null : phone.replaceAll("[^0-9]", "");
    }
}
