package com.drf.order.entity;

import com.drf.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "order_item")
public class OrderItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false, length = 100)
    private String productName;

    @Column(nullable = false)
    private int unitPrice;

    @Column(nullable = false)
    private int discountedPrice;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private int productCouponDiscountAmount;

    @Column(nullable = false)
    private int orderCouponDiscountAmount;

    @Column(nullable = false)
    private int finalAmount;

    private Long memberCouponId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderItemStatus status;

    public void updateStatus(OrderItemStatus status) {
        this.status = status;
    }
}
