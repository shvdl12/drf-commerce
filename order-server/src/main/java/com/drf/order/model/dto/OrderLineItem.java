package com.drf.order.model.dto;

import com.drf.common.model.Money;
import com.drf.order.client.dto.response.InternalProductResponse;
import com.drf.order.entity.CartItem;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class OrderLineItem {

    private final long cartItemId;
    private final long productId;
    private final String productName;
    private final Money unitPrice; // 개당 가격
    private final Money unitDiscountAmount; // 자체 할인 금액
    private final Money discountedUnitPrice; // 자체 할인이 반영된 개당 가격
    private final int quantity;
    private final Long memberCouponId;
    private final List<Long> categoryPath;

    private Money productCouponDiscount;
    private Money orderCouponDiscount;

    public static OrderLineItem of(CartItem cartItem, InternalProductResponse product) {
        return OrderLineItem.builder()
                .cartItemId(cartItem.getId())
                .productId(cartItem.getProductId())
                .productName(product.name())
                .unitPrice(Money.of(product.price()))
                .unitDiscountAmount(Money.of(product.discountAmount()))
                .discountedUnitPrice(Money.of(product.discountedPrice()))
                .quantity(cartItem.getQuantity())
                .memberCouponId(cartItem.getCouponId())
                .categoryPath(product.categoryPath())
                .build();
    }

    public void applyProductCouponDiscount(Money discount) {
        this.productCouponDiscount = discount;
    }

    public void applyOrderCouponDiscount(Money discount) {
        this.orderCouponDiscount = discount;
    }

    public Money grossAmount() {
        return unitPrice.multiply(quantity);
    }

    public Money getLineAmount() {
        return discountedUnitPrice.multiply(quantity).subtract(productCouponDiscount);
    }

    public Money getProductDiscountAmount() {
        return unitDiscountAmount.multiply(quantity);
    }

    public OrderItemData toOrderItemData() {
        return OrderItemData.builder()
                .productId(productId)
                .productName(productName)
                .unitPrice(unitPrice)
                .discountedPrice(discountedUnitPrice)
                .quantity(quantity)
                .productCouponDiscountAmount(productCouponDiscount)
                .orderCouponDiscountAmount(orderCouponDiscount)
                .finalAmount(discountedUnitPrice.multiply(quantity).subtract(productCouponDiscount).subtract(orderCouponDiscount))
                .memberCouponId(memberCouponId)
                .build();
    }
}
