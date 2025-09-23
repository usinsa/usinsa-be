package com.usinsa.backend.domain.product.entity;

import com.usinsa.backend.domain.category.entity.Category;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) // 다대일 관계 (여러 상품 -> 하나의 카테고리)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "product_name", nullable = false, length = 100)
    private String name;

    @Column(name = "brand_name", nullable = false, length = 100)
    private String brandName;

    @Column(name = "product_price", nullable = false)
    private Integer price;

    @Column(name = "product_like")
    private Integer likeCount;

    @Column(name = "product_click")
    private Integer clickCount;

    // 연관관계 설정
    public void setCategory(Category category) {
        this.category = category;
    }

}
