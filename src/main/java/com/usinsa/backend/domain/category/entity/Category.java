package com.usinsa.backend.domain.category.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "category")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Long id;

    // parent_id: NULL -> 상위 카테고리
    // parent_id: 존재 -> 하위 카테고리
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    @OneToMany(mappedBy = "parent")
    private List<Category> children = new ArrayList<>();

    @Column(name = "category_name", nullable = false, length = 100)
    private String name;

    // 연관관계 설정
    public void addChild(Category child) {
        this.children.add(child);
        child.parent = this;
    }
}
