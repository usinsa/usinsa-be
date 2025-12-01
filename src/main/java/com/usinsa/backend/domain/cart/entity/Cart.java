package com.usinsa.backend.domain.cart.entity;

import com.usinsa.backend.domain.member.entity.Member;
import com.usinsa.backend.domain.product.entity.ProductOption;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cart")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_option_id", nullable = false)
    private ProductOption productOption;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = true)
    private Member member;

    @Column(name = "session_id", length = 255)
    private String sessionId;

    @Column(nullable = false)
    private Integer count;

    public void setCount(int count) {
        this.count = count;
    }

    public void setMember(Member member) {
        this.member = member;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public boolean isGuestCart() {
        return member == null && sessionId != null;
    }

    public boolean isMemberCart() {
        return member != null;
    }
}