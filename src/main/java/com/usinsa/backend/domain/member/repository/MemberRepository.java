package com.usinsa.backend.domain.member.repository;

import com.usinsa.backend.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    boolean existsByEmail(String email);
    boolean existsByUsinaId(String usinaId);

    Optional<Member> findByUsinaId(String usinaId);
    Optional<Member> findByEmail(String email);
}
