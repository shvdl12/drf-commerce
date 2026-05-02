package com.drf.coupon.service;

import com.drf.common.model.Money;
import com.drf.coupon.calculator.CartCouponCalculator;
import com.drf.coupon.entity.ApplyType;
import com.drf.coupon.entity.MemberCoupon;
import com.drf.coupon.entity.MemberCouponStatus;
import com.drf.coupon.facade.InternalCouponFacade;
import com.drf.coupon.model.request.internal.InternalProductCouponRequest;
import com.drf.coupon.model.response.internal.CartCouponResult;
import com.drf.coupon.model.response.internal.InternalCartCouponCalculateResponse;
import com.drf.coupon.model.response.internal.InternalCartCouponListResponse;
import com.drf.coupon.model.response.internal.InternalProductCouponListResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class InternalCouponFacadeTest {

    @InjectMocks
    private InternalCouponFacade internalCouponFacade;

    @Mock
    private InternalCouponService internalCouponService;

    @Mock
    private CartCouponCalculator cartCouponCalculator;

    private MemberCoupon stubMemberCoupon(long id) {
        return MemberCoupon.builder()
                .id(id).memberId(1L)
                .status(MemberCouponStatus.UNUSED)
                .build();
    }

    private CartCouponResult couponResult(long memberCouponId, Money discountAmount) {
        return new CartCouponResult(memberCouponId, "쿠폰" + memberCouponId, discountAmount, List.of());
    }

    @Nested
    @DisplayName("calculateCartCoupon")
    class CalculateCartCouponTest {

        @Test
        @DisplayName("쿠폰 조건 미달로 계산 결과 없으면 applicable: false")
        void notApplicable_whenCalculatorReturnsEmpty() {
            MemberCoupon mc = stubMemberCoupon(1L);
            given(internalCouponService.getUnusedMemberCoupon(1L, 1L)).willReturn(mc);
            given(cartCouponCalculator.calculate(eq(List.of(mc)), any()))
                    .willReturn(new InternalCartCouponListResponse(List.of()));

            InternalCartCouponCalculateResponse result =
                    internalCouponFacade.calculateCartCoupon(1L, 1L, List.of());

            assertThat(result.applicable()).isFalse();
            assertThat(result.totalDiscountAmount()).isZero();
            assertThat(result.items()).isEmpty();
        }

        @Test
        @DisplayName("계산 결과 있으면 applicable: true, 할인금액 반환")
        void applicable_whenCalculatorReturnsResult() {
            MemberCoupon mc = stubMemberCoupon(1L);
            CartCouponResult cr = couponResult(1L, Money.of(3000));
            given(internalCouponService.getUnusedMemberCoupon(1L, 1L)).willReturn(mc);
            given(cartCouponCalculator.calculate(eq(List.of(mc)), any()))
                    .willReturn(new InternalCartCouponListResponse(List.of(cr)));

            InternalCartCouponCalculateResponse result =
                    internalCouponFacade.calculateCartCoupon(1L, 1L, List.of());

            assertThat(result.applicable()).isTrue();
            assertThat(result.totalDiscountAmount()).isEqualTo(3000);
        }
    }

    @Nested
    @DisplayName("getAvailableProductCoupons")
    class GetAvailableProductCouponsTest {

        private InternalProductCouponRequest request(List<Long> usedIds) {
            return new InternalProductCouponRequest(
                    1L, 10L, 100L, 50000, 1, List.of(10L, 20L), usedIds);
        }

        @Test
        @DisplayName("usedMemberCouponIds에 포함된 쿠폰은 usedOnOtherItem: true")
        void usedOnOtherItem_whenCouponIdInUsedList() {
            CartCouponResult cr = couponResult(5L, Money.of(2000));
            given(internalCouponService.getUnusedCouponsByType(1L, ApplyType.PRODUCT))
                    .willReturn(List.of(stubMemberCoupon(5L)));
            given(cartCouponCalculator.calculate(any(), any()))
                    .willReturn(new InternalCartCouponListResponse(List.of(cr)));

            InternalProductCouponListResponse result =
                    internalCouponFacade.getAvailableProductCoupons(request(List.of(5L)));

            assertThat(result.coupons()).hasSize(1);
            assertThat(result.coupons().get(0).usedOnOtherItem()).isTrue();
        }

        @Test
        @DisplayName("usedMemberCouponIds에 없는 쿠폰은 usedOnOtherItem: false")
        void notUsedOnOtherItem_whenCouponIdNotInUsedList() {
            CartCouponResult cr = couponResult(5L, Money.of(2000));
            given(internalCouponService.getUnusedCouponsByType(1L, ApplyType.PRODUCT))
                    .willReturn(List.of(stubMemberCoupon(5L)));
            given(cartCouponCalculator.calculate(any(), any()))
                    .willReturn(new InternalCartCouponListResponse(List.of(cr)));

            InternalProductCouponListResponse result =
                    internalCouponFacade.getAvailableProductCoupons(request(List.of()));

            assertThat(result.coupons().get(0).usedOnOtherItem()).isFalse();
        }

        @Test
        @DisplayName("isBest가 true인 쿠폰은 응답에도 isBest: true")
        void isBest_propagatedFromCalculatorResult() {
            CartCouponResult cr = couponResult(5L, Money.of(2000));
            cr.markAsBest();
            given(internalCouponService.getUnusedCouponsByType(1L, ApplyType.PRODUCT))
                    .willReturn(List.of(stubMemberCoupon(5L)));
            given(cartCouponCalculator.calculate(any(), any()))
                    .willReturn(new InternalCartCouponListResponse(List.of(cr)));

            InternalProductCouponListResponse result =
                    internalCouponFacade.getAvailableProductCoupons(request(List.of()));

            assertThat(result.coupons().getFirst().isBest()).isTrue();
        }

        @Test
        @DisplayName("PRODUCT 타입 쿠폰만 조회")
        void queriesOnlyProductTypeCoupons() {
            given(internalCouponService.getUnusedCouponsByType(1L, ApplyType.PRODUCT))
                    .willReturn(List.of());
            given(cartCouponCalculator.calculate(any(), any()))
                    .willReturn(new InternalCartCouponListResponse(List.of()));

            internalCouponFacade.getAvailableProductCoupons(request(List.of()));

            // PRODUCT 타입으로만 조회했는지 검증
            org.mockito.BDDMockito.then(internalCouponService).should()
                    .getUnusedCouponsByType(1L, ApplyType.PRODUCT);
        }
    }
}
