package com.usinsa.backend.domain.product.entity;

import com.usinsa.backend.domain.category.entity.Category;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

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
    private Long price;

    @Column(name = "product_like")
    private Integer likeCount;

    @Column(name = "product_click")
    private Integer clickCount;

    /* cascade = CascadeType.ALL
     ->Product를 저장/삭제할 때 관련된 ProductOption들도 같이 저장/삭제됨
       orphanRemoval = true
     ->Product의 options 컬렉션에서 제거된 ProductOption은 DB에서도 자동 삭제됨 */
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductOption> options = new ArrayList<>();

    // 연관관계 설정
    public void setCategory(Category category) {
        this.category = category;
    }
    public void addOption(ProductOption option) {
        options.add(option);
        option.setProduct(this);
    }

}
