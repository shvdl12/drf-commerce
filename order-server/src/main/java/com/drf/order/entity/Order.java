package com.drf.order.entity;

import com.drf.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "`order`")
public class Order extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String orderNo;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private int totalAmount;

    @Column(nullable = false)
    private int deliveryFee;

    @Column(nullable = false)
    private int productDiscountAmount;

    @Column(nullable = false)
    private int couponDiscountAmount;

    @Column(nullable = false)
    private int finalAmount;

    @Column(nullable = false)
    private int refundedAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    private Long memberCouponId;

    // 배송지 스냅샷
    @Column(nullable = false, length = 50)
    private String receiverName;

    @Column(nullable = false, length = 20)
    private String receiverPhone;

    @Column(nullable = false, length = 10)
    private String zipCode;

    @Column(nullable = false, length = 255)
    private String address;

    @Column(nullable = false, length = 100)
    private String addressDetail;

    public void updateStatus(OrderStatus status) {
        this.status = status;
    }

    public void addRefundedAmount(int amount) {
        this.refundedAmount += amount;
    }
}
