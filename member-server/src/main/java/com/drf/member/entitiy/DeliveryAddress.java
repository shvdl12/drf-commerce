package com.drf.member.entitiy;

import com.drf.member.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "delivery_address")
public class DeliveryAddress extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(nullable = false, length = 255)
    private String address;

    @Column(nullable = false, length = 100)
    private String addressDetail;

    @Column(nullable = false, length = 10)
    private String zipCode;

    @Column(nullable = false)
    private boolean isDefault;

    public void unmarkDefault() {
        this.isDefault = false;
    }

    public void update(String name, String phone, String address, String addressDetail, String zipCode, boolean isDefault) {
        this.name = name;
        this.phone = phone;
        this.address = address;
        this.addressDetail = addressDetail;
        this.zipCode = zipCode;
        this.isDefault = isDefault;
    }
}
