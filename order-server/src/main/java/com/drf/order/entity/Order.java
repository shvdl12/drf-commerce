package com.drf.order.entity;

import com.drf.common.converter.MoneyConverter;
import com.drf.common.entity.BaseTimeEntity;
import com.drf.common.model.Money;
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
    @Convert(converter = MoneyConverter.class)
    private Money totalAmount;

    @Column(nullable = false)
    @Convert(converter = MoneyConverter.class)
    private Money deliveryFee;

    @Column(nullable = false)
    @Convert(converter = MoneyConverter.class)
    private Money productDiscountAmount;

    @Column(nullable = false)
    @Convert(converter = MoneyConverter.class)
    private Money couponDiscountAmount;

    @Column(nullable = false)
    @Convert(converter = MoneyConverter.class)
    private Money finalAmount;

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
}
