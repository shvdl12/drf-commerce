package com.drf.coupon.calculator;

import com.drf.coupon.discount.*;
import com.drf.coupon.entity.*;
import com.drf.coupon.model.request.internal.InternalCartCouponItemRequest;
import com.drf.coupon.model.response.internal.CartCouponResult;
import com.drf.coupon.model.response.internal.InternalCartCouponListResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CartCouponCalculatorTest {

    private CartCouponCalculator calculator;

    @BeforeEach
    void setUp() {
        DiscountStrategyRegistry discountStrategyRegistry = new DiscountStrategyRegistry(
                List.of(new FixedDiscountStrategy(), new RateDiscountStrategy()));
        ApplyScopeRegistry applyScopeRegistry = new ApplyScopeRegistry(
                List.of(new AllApplyScopeStrategy(), new CategoryApplyScope()));
        calculator = new CartCouponCalculator(discountStrategyRegistry, applyScopeRegistry);
    }

    private Coupon fixedCoupon(int discountValue, int minOrderAmount, int minOrderQuantity,
                               ApplyScope scope, Long applyTargetId) {
        return Coupon.builder()
                .id(1L).name("정액쿠폰")
                .discountType(DiscountType.FIXED).discountValue(discountValue)
                .totalQuantity(100).issuedQuantity(0)
                .minOrderAmount(minOrderAmount).minOrderQuantity(minOrderQuantity)
                .applyType(ApplyType.ORDER).applyScope(scope).applyTargetId(applyTargetId)
                .isUnlimited(false).maxIssuablePerMember(1)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validUntil(LocalDateTime.now().plusDays(30))
                .status(CouponStatus.ACTIVE)
                .build();
    }

    private Coupon rateCoupon(int rate, Integer maxDiscount, int minOrderAmount,
                              ApplyScope scope, Long applyTargetId) {
        return Coupon.builder()
                .id(2L).name("정률쿠폰")
                .discountType(DiscountType.RATE).discountValue(rate)
                .totalQuantity(100).issuedQuantity(0)
                .minOrderAmount(minOrderAmount).minOrderQuantity(1)
                .maxDiscountAmount(maxDiscount)
                .applyType(ApplyType.ORDER).applyScope(scope).applyTargetId(applyTargetId)
                .isUnlimited(false).maxIssuablePerMember(1)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validUntil(LocalDateTime.now().plusDays(30))
                .status(CouponStatus.ACTIVE)
                .build();
    }

    private MemberCoupon memberCoupon(long id, Coupon coupon) {
        return MemberCoupon.builder()
                .id(id).coupon(coupon).memberId(1L)
                .status(MemberCouponStatus.UNUSED)
                .build();
    }

    private InternalCartCouponItemRequest item(long cartItemId, long productId, int price, int quantity, List<Long> categoryPath) {
        return new InternalCartCouponItemRequest(cartItemId, productId, price, quantity, categoryPath);
    }

    @Nested
    @DisplayName("ALL scope")
    class AllScopeTest {

        @Test
        @DisplayName("정액 쿠폰: 전체 상품에 금액 비율로 안분")
        void fixed_distributesByRatio() {
            Coupon coupon = fixedCoupon(1000, 0, 0, ApplyScope.ALL, null);
            // 상품A 30000(30%), 상품B 70000(70%) → 300 + 700
            List<InternalCartCouponItemRequest> items = List.of(
                    item(1L, 1L, 30000, 1, List.of(10L)),
                    item(2L, 2L, 70000, 1, List.of(10L))
            );

            InternalCartCouponListResponse result = calculator.calculate(List.of(memberCoupon(1L, coupon)), items);

            assertThat(result.coupons()).hasSize(1);
            CartCouponResult cr = result.coupons().getFirst();
            assertThat(cr.getDiscountAmount()).isEqualTo(1000);
            assertThat(cr.getItems()).satisfiesExactly(
                    i -> { assertThat(i.appliedYn()).isTrue(); assertThat(i.discountAmount()).isEqualTo(300); },
                    i -> { assertThat(i.appliedYn()).isTrue(); assertThat(i.discountAmount()).isEqualTo(700); }
            );
        }

        @Test
        @DisplayName("정률 쿠폰: 전체 금액 기준 계산")
        void rate_appliesOnTotalAmount() {
            Coupon coupon = rateCoupon(10, null, 0, ApplyScope.ALL, null);
            List<InternalCartCouponItemRequest> items = List.of(item(1L, 1L, 50000, 1, List.of(10L)));

            int discountAmount = calculator.calculate(List.of(memberCoupon(1L, coupon)), items)
                    .coupons().get(0).getDiscountAmount();

            assertThat(discountAmount).isEqualTo(5000);
        }

        @Test
        @DisplayName("정률 쿠폰: 최대 할인 상한 적용")
        void rate_cappedByMaxDiscount() {
            Coupon coupon = rateCoupon(10, 3000, 0, ApplyScope.ALL, null);
            List<InternalCartCouponItemRequest> items = List.of(item(1L, 1L, 100000, 1, List.of(10L)));

            int discountAmount = calculator.calculate(List.of(memberCoupon(1L, coupon)), items)
                    .coupons().get(0).getDiscountAmount();

            assertThat(discountAmount).isEqualTo(3000);
        }
    }

    @Nested
    @DisplayName("CATEGORY scope")
    class CategoryScopeTest {

        @Test
        @DisplayName("매칭 상품만 appliedYn=true, 나머지 false/0원")
        void appliedYn_onlyMatchingItems() {
            Coupon coupon = fixedCoupon(2000, 0, 0, ApplyScope.CATEGORY, 20L);
            List<InternalCartCouponItemRequest> items = List.of(
                    item(1L, 1L, 30000, 1, List.of(10L, 20L, 30L)),  // 매칭
                    item(2L, 2L, 70000, 1, List.of(10L, 21L, 40L))   // 비매칭
            );

            CartCouponResult cr = calculator.calculate(List.of(memberCoupon(1L, coupon)), items)
                    .coupons().get(0);

            assertThat(cr.getItems()).satisfiesExactly(
                    i -> { assertThat(i.appliedYn()).isTrue(); assertThat(i.discountAmount()).isEqualTo(2000); },
                    i -> { assertThat(i.appliedYn()).isFalse(); assertThat(i.discountAmount()).isEqualTo(0); }
            );
        }

        @Test
        @DisplayName("최소 주문금액은 카테고리 매칭 합계 기준 — 전체 합계가 충족해도 매칭 합계 미달이면 제외")
        void minOrderAmount_basedOnCategoryAmount() {
            // 매칭 금액 30000, 전체 100000, 최소 주문금액 50000 → 제외
            Coupon coupon = fixedCoupon(2000, 50000, 0, ApplyScope.CATEGORY, 20L);
            List<InternalCartCouponItemRequest> items = List.of(
                    item(1L, 1L, 30000, 1, List.of(10L, 20L, 30L)),
                    item(2L, 2L, 70000, 1, List.of(10L, 21L, 40L))
            );

            assertThat(calculator.calculate(List.of(memberCoupon(1L, coupon)), items).coupons()).isEmpty();
        }

        @Test
        @DisplayName("카테고리 매칭 상품 없으면 쿠폰 제외")
        void noMatchingItems_excludedFromResult() {
            Coupon coupon = fixedCoupon(2000, 0, 0, ApplyScope.CATEGORY, 99L);
            List<InternalCartCouponItemRequest> items = List.of(item(1L, 1L, 30000, 1, List.of(10L, 20L)));

            assertThat(calculator.calculate(List.of(memberCoupon(1L, coupon)), items).coupons()).isEmpty();
        }
    }

    @Nested
    @DisplayName("조건 검증")
    class ConditionTest {

        @Test
        @DisplayName("최소 주문금액 미달 시 제외")
        void belowMinOrderAmount_excluded() {
            Coupon coupon = fixedCoupon(3000, 100000, 0, ApplyScope.ALL, null);
            List<InternalCartCouponItemRequest> items = List.of(item(1L, 1L, 50000, 1, List.of(10L)));

            assertThat(calculator.calculate(List.of(memberCoupon(1L, coupon)), items).coupons()).isEmpty();
        }

        @Test
        @DisplayName("최소 수량 미달 시 제외")
        void belowMinOrderQuantity_excluded() {
            Coupon coupon = fixedCoupon(3000, 0, 3, ApplyScope.ALL, null);
            List<InternalCartCouponItemRequest> items = List.of(item(1L, 1L, 50000, 2, List.of(10L)));

            assertThat(calculator.calculate(List.of(memberCoupon(1L, coupon)), items).coupons()).isEmpty();
        }

        @Test
        @DisplayName("만료된 쿠폰 제외")
        void expiredCoupon_excluded() {
            Coupon coupon = Coupon.builder()
                    .id(1L).name("만료쿠폰")
                    .discountType(DiscountType.FIXED).discountValue(3000)
                    .totalQuantity(100).issuedQuantity(0)
                    .minOrderAmount(0).minOrderQuantity(1)
                    .applyType(ApplyType.ORDER).applyScope(ApplyScope.ALL)
                    .isUnlimited(false).maxIssuablePerMember(1)
                    .validFrom(LocalDateTime.now().minusDays(10))
                    .validUntil(LocalDateTime.now().minusDays(1))
                    .status(CouponStatus.ACTIVE)
                    .build();
            List<InternalCartCouponItemRequest> items = List.of(item(1L, 1L, 50000, 1, List.of(10L)));

            assertThat(calculator.calculate(List.of(memberCoupon(1L, coupon)), items).coupons()).isEmpty();
        }
    }

    @Nested
    @DisplayName("isBest 마킹")
    class IsBestTest {

        @Test
        @DisplayName("여러 쿠폰 중 할인금액이 가장 큰 쿠폰이 isBest, 내림차순 정렬")
        void isBest_markedOnHighestDiscount() {
            Coupon cheap = fixedCoupon(1000, 0, 0, ApplyScope.ALL, null);
            Coupon expensive = Coupon.builder()
                    .id(2L).name("5000원쿠폰")
                    .discountType(DiscountType.FIXED).discountValue(5000)
                    .totalQuantity(100).issuedQuantity(0)
                    .minOrderAmount(0).minOrderQuantity(1)
                    .applyType(ApplyType.ORDER).applyScope(ApplyScope.ALL)
                    .isUnlimited(false).maxIssuablePerMember(1)
                    .validFrom(LocalDateTime.now().minusDays(1))
                    .validUntil(LocalDateTime.now().plusDays(30))
                    .status(CouponStatus.ACTIVE)
                    .build();
            List<MemberCoupon> coupons = List.of(memberCoupon(1L, cheap), memberCoupon(2L, expensive));
            List<InternalCartCouponItemRequest> items = List.of(item(1L, 1L, 50000, 1, List.of(10L)));

            InternalCartCouponListResponse result = calculator.calculate(coupons, items);

            assertThat(result.coupons()).hasSize(2);
            assertThat(result.coupons().get(0).getDiscountAmount()).isEqualTo(5000);
            assertThat(result.coupons().get(0).isBest()).isTrue();
            assertThat(result.coupons().get(1).isBest()).isFalse();
        }

        @Test
        @DisplayName("쿠폰이 하나면 그 쿠폰이 isBest")
        void isBest_singleCoupon() {
            Coupon coupon = fixedCoupon(3000, 0, 0, ApplyScope.ALL, null);
            List<InternalCartCouponItemRequest> items = List.of(item(1L, 1L, 50000, 1, List.of(10L)));

            assertThat(calculator.calculate(List.of(memberCoupon(1L, coupon)), items)
                    .coupons().get(0).isBest()).isTrue();
        }
    }
}
