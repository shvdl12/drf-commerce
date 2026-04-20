package com.drf.coupon.service;

import com.drf.coupon.entity.ApplyType;
import com.drf.coupon.entity.Coupon;
import com.drf.coupon.entity.CouponStatus;
import com.drf.coupon.entity.DiscountType;
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
                .applyType(ApplyType.ORDER)
                .maxIssuablePerMember(1)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validUntil(LocalDateTime.now().plusDays(30))
                .status(CouponStatus.ACTIVE)
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
}
