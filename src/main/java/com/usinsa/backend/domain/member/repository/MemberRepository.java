package com.usinsa.backend.domain.member.repository;

import com.usinsa.backend.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {
    boolean existsByEmail(String email);
    boolean existsByUsinaId(String usinaId);

}
