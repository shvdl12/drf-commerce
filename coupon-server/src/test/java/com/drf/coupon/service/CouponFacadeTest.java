package com.drf.coupon.service;

import com.drf.coupon.entity.*;
import com.drf.coupon.model.response.CouponCalculateResponse;
import com.drf.coupon.model.response.CouponIssueResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class CouponFacadeTest {

    @InjectMocks
    private CouponFacade couponFacade;

    @Mock
    private CouponService couponService;

    private Coupon coupon() {
        return Coupon.builder()
                .id(1L)
                .name("신규 가입 쿠폰")
                .discountType(DiscountType.FIXED)
                .discountValue(3000)
                .totalQuantity(100)
                .issuedQuantity(0)
                .minOrderAmount(10000)
                .applyType(ApplyType.ALL)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validUntil(LocalDateTime.now().plusDays(30))
                .status(CouponStatus.ACTIVE)
                .build();
    }

    private MemberCoupon memberCoupon(Coupon coupon) {
        return MemberCoupon.builder()
                .id(1L)
                .coupon(coupon)
                .memberId(1L)
                .status(MemberCouponStatus.UNUSED)
                .build();
    }

    @Test
    @DisplayName("issueCoupon - 검증 후 발급 순서로 서비스 호출")
    void issueCoupon_callsValidateThenIssue() {
        // given
        Coupon coupon = coupon();
        given(couponService.getCouponForIssue(1L, 1L)).willReturn(coupon);
        given(couponService.issueCoupon(coupon, 1L)).willReturn(new CouponIssueResponse(10L));

        // when
        CouponIssueResponse result = couponFacade.issueCoupon(1L, 1L);

        // then
        assertThat(result.memberCouponId()).isEqualTo(10L);
        then(couponService).should().getCouponForIssue(1L, 1L);
        then(couponService).should().issueCoupon(coupon, 1L);
    }

    @Test
    @DisplayName("calculateCoupon - fetch 후 calculate 순서로 서비스 호출")
    void calculateCoupon_callsFetchThenCalculate() {
        // given
        Coupon coupon = coupon();
        MemberCoupon memberCoupon = memberCoupon(coupon);
        CouponCalculateResponse expected = new CouponCalculateResponse(15000, 3000, 12000);

        given(couponService.getMemberCoupon(1L, 1L)).willReturn(memberCoupon);
        given(couponService.calculate(memberCoupon, 15000, null)).willReturn(expected);

        // when
        CouponCalculateResponse result = couponFacade.calculateCoupon(1L, 1L, 15000, null);

        // then
        assertThat(result).isEqualTo(expected);
        then(couponService).should().getMemberCoupon(1L, 1L);
        then(couponService).should().calculate(memberCoupon, 15000, null);
    }
}
