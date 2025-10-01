package com.usinsa.backend.domain.order.entity;

import com.usinsa.backend.domain.product.entity.ProductOption;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ordered_product")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class OrderedProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ordered_product_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_option_id", nullable = false)
    private ProductOption productOption;

    @Column(nullable = false)
    private Integer quantity;

    public void setQuantity(Integer newQuantity) {
        this.quantity = newQuantity;
    }
}