package com.drf.coupon.strategy;

import com.drf.common.model.Money;
import com.drf.coupon.entity.ApplyType;
import com.drf.coupon.entity.Coupon;
import com.drf.coupon.entity.CouponStatus;
import com.drf.coupon.entity.DiscountType;
import com.drf.coupon.strategy.discount.FixedDiscountStrategy;
import com.drf.coupon.strategy.discount.RateDiscountStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class DiscountStrategyTest {

    private Coupon coupon(DiscountType discountType, int discountValue,
                          ApplyType applyType, Long applyTargetId, Long maxDiscountAmount) {

        Money _maxDiscountAmount = maxDiscountAmount != null ? Money.of(maxDiscountAmount) : null;

        return Coupon.builder()
                .id(1L)
                .name("테스트 쿠폰")
                .discountType(discountType)
                .discountValue(discountValue)
                .totalQuantity(100)
                .issuedQuantity(0)
                .minOrderAmount(Money.of(10000))
                .maxDiscountAmount(_maxDiscountAmount)
                .applyType(applyType)
                .applyTargetId(applyTargetId)
                .maxIssuablePerMember(1)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validUntil(LocalDateTime.now().plusDays(30))
                .status(CouponStatus.ACTIVE)
                .build();
    }

    @Nested
    @DisplayName("정액 할인 정책")
    class FixedDiscountStrategyTest {

        private final FixedDiscountStrategy policy = new FixedDiscountStrategy();

        @Test
        @DisplayName("base 금액에 관계없이 고정 금액 반환")
        void calculate_returnsFixedValue() {
            Coupon coupon = coupon(DiscountType.FIXED, 3000, ApplyType.ORDER, null, null);

            assertThat(policy.calculate(coupon, Money.of(15000)).toLong()).isEqualTo(3000);
            assertThat(policy.calculate(coupon, Money.of(50000)).toLong()).isEqualTo(3000);
        }
    }

    @Nested
    @DisplayName("정률 할인 정책")
    class RateDiscountStrategyTest {

        private final RateDiscountStrategy policy = new RateDiscountStrategy();

        @Test
        @DisplayName("base 금액에 비율 적용")
        void calculate_rate() {
            Coupon coupon = coupon(DiscountType.RATE, 10, ApplyType.ORDER, null, null);

            assertThat(policy.calculate(coupon, Money.of(20000)).toLong()).isEqualTo(2000);
        }

        @Test
        @DisplayName("최대 할인 금액 상한 적용")
        void calculate_capByMax() {
            Coupon coupon = coupon(DiscountType.RATE, 10, ApplyType.ORDER, null, 5000L);

            assertThat(policy.calculate(coupon, Money.of(100000)).toLong()).isEqualTo(5000);
        }

        @Test
        @DisplayName("최대 할인 금액 없으면 계산값 그대로")
        void calculate_noMax() {
            Coupon coupon = coupon(DiscountType.RATE, 10, ApplyType.ORDER, null, null);

            assertThat(policy.calculate(coupon, Money.of(100000)).toLong()).isEqualTo(10000);
        }
    }

}
