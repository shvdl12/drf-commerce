package com.drf.coupon.calculator;

import com.drf.common.exception.BusinessException;
import com.drf.common.model.Money;
import com.drf.coupon.entity.Coupon;
import com.drf.coupon.entity.MemberCoupon;
import com.drf.coupon.model.request.internal.InternalCartCouponItemRequest;
import com.drf.coupon.model.response.internal.CartCouponResult;
import com.drf.coupon.model.response.internal.InternalCartCouponListResponse;
import com.drf.coupon.model.response.internal.InternalCouponItemResult;
import com.drf.coupon.strategy.discount.DiscountStrategy;
import com.drf.coupon.strategy.discount.DiscountStrategyRegistry;
import com.drf.coupon.strategy.scope.ApplyScopeRegistry;
import com.drf.coupon.strategy.scope.ApplyScopeStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CartCouponCalculator {

    private final DiscountStrategyRegistry discountStrategyRegistry;
    private final ApplyScopeRegistry applyScopeRegistry;

    public InternalCartCouponListResponse calculate(
            List<MemberCoupon> memberCoupons, List<InternalCartCouponItemRequest> items) {
        List<CartCouponResult> results = new ArrayList<>();

        for (MemberCoupon mc : memberCoupons) {
            Coupon coupon = mc.getCoupon();

            // 만료·사용불가 쿠폰 제외
            try {
                coupon.validateCouponAvailability();
            } catch (BusinessException e) {
                continue;
            }

            ApplyScopeStrategy scopeStrategy = applyScopeRegistry.get(coupon.getApplyScope());
            DiscountStrategy discountStrategy = discountStrategyRegistry.get(coupon.getDiscountType());

            // 적용 상품 리스트 추출 및 총 금액, 수량 계산
            List<InternalCartCouponItemRequest> applicableItems =
                    scopeStrategy.filterApplicableItems(items, coupon.getApplyTargetId());

            if (applicableItems.isEmpty()) continue;

            Money applicableAmount = calculateApplicableAmount(applicableItems);
            int applicableQuantity = calculateApplicableQuantity(applicableItems);

            // 최소 주문 금액·수량 미달 시 제외
            if (applicableAmount.isLessThanOrEqualTo(coupon.getMinOrderAmount())) continue;
            if (applicableQuantity < coupon.getMinOrderQuantity()) continue;

            Money discountAmount = discountStrategy.calculate(coupon, applicableAmount);
            if (discountAmount.isNegative()) continue;

            // 총 할인액을 상품별 금액 비율로 안분
            List<InternalCouponItemResult> itemResults = distributeDiscount(
                    items, applicableItems, applicableAmount, discountAmount);

            results.add(new CartCouponResult(mc.getId(), coupon.getName(), discountAmount, itemResults));
        }

        // 할인금액 내림차순 정렬 후 가장 유리한 쿠폰에 isBest 마킹
        results.sort(Comparator.comparingLong(CartCouponResult::getDiscountAmount).reversed());
        if (!results.isEmpty()) {
            results.getFirst().markAsBest();
        }

        return new InternalCartCouponListResponse(results);
    }

    private List<InternalCouponItemResult> distributeDiscount(
            List<InternalCartCouponItemRequest> items,
            List<InternalCartCouponItemRequest> applicableItems,
            Money applicableAmount,
            Money discountAmount) {

        Set<Long> applicableIds = applicableItems.stream()
                .map(InternalCartCouponItemRequest::cartItemId)
                .collect(Collectors.toSet());

        List<InternalCouponItemResult> results = new ArrayList<>(items.size());
        Money distributed = Money.ZERO;
        int processedCount = 0;
        int totalApplicable = applicableItems.size();

        for (InternalCartCouponItemRequest item : items) {
            boolean isApplicable = applicableIds.contains(item.cartItemId());
            Money itemDiscount = Money.ZERO;

            if (isApplicable) {
                processedCount++;

                // 마지막 적용 상품인 경우 남은 자투리 금액을 모두 할당 (정수 나눗셈 오차 보정)
                if (processedCount == totalApplicable) {
                    itemDiscount = discountAmount.subtract(distributed);
                } else {
                    Money itemAmount = Money.of(item.price()).multiply(item.quantity());
                    itemDiscount = discountAmount.proportionOf(itemAmount, applicableAmount);
                }
                distributed = distributed.add(itemDiscount);
            }

            results.add(new InternalCouponItemResult(
                    item.cartItemId(),
                    item.productId(),
                    isApplicable,
                    itemDiscount.toLong()
            ));
        }

        return results;
    }

    private Money calculateApplicableAmount(List<InternalCartCouponItemRequest> items) {
        return items.stream()
                .map(i -> Money.of(i.price()).multiply(i.quantity()))
                .reduce(Money.ZERO, Money::add);
    }

    private int calculateApplicableQuantity(List<InternalCartCouponItemRequest> items) {
        return items.stream()
                .mapToInt(InternalCartCouponItemRequest::quantity)
                .sum();
    }
}
