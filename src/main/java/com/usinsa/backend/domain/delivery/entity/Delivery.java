package com.usinsa.backend.domain.delivery.entity;


import com.usinsa.backend.domain.order.entity.Order;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "delivery")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Delivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "delivery_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @Column(name = "tracking_number", nullable = false, length = 16)
    private String trackingNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", nullable = false)
    private DeliveryStatus deliveryStatus;

    // 연관관계 설정
    public void setOrder(Order order) {
        this.order = order;
        if (order.getDelivery() != this) {
            order.setDelivery(this);
        }
    }

    public void updateDeliveryStatus(DeliveryStatus newStatus) {
        this.deliveryStatus = newStatus;
    }

    public void updateTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }
}