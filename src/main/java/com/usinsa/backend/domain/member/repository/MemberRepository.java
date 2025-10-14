package com.usinsa.backend.domain.member.repository;

import com.usinsa.backend.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/*
 * JpaRepository를 상속할 때,
 * 제네릭스 타입으로 reopository의 대상이되는 Entity의 타입과 해당 PK의 속성 타입을 정의
 * Member와 Member PK인 Long 타입 = <Member, Long>
 */
public interface MemberRepository extends JpaRepository<Member, Long> {
    boolean existsByEmail(String email);
    boolean existsByUsinaId(String usinaId);

    Optional<Member> findByUsinaId(String usinaId);
}
