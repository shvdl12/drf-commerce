package com.drf.order.entity;

import com.drf.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "cart",
        uniqueConstraints = @UniqueConstraint(columnNames = {"member_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cart extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long memberId;

    private Long couponId;

    private Cart(Long memberId) {
        this.memberId = memberId;
    }

    public static Cart of(Long memberId) {
        return new Cart(memberId);
    }
}
