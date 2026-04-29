package com.drf.order.model.dto;

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
    private final int unitPrice; // 개당 가격
    private final int unitDiscountAmount; // 자체 할인 금액
    private final int discountedUnitPrice; // 자체 할인이 반영된 개당 가격
    private final int quantity;
    private final Long memberCouponId;
    private final List<Long> categoryPath;

    private int productCouponDiscount;
    private int orderCouponDiscount;

    public static OrderLineItem of(CartItem cartItem, InternalProductResponse product) {
        return OrderLineItem.builder()
                .cartItemId(cartItem.getId())
                .productId(cartItem.getProductId())
                .productName(product.name())
                .unitPrice(product.price())
                .unitDiscountAmount(product.discountAmount())
                .discountedUnitPrice(product.discountedPrice())
                .quantity(cartItem.getQuantity())
                .memberCouponId(cartItem.getCouponId())
                .categoryPath(product.categoryPath())
                .build();
    }

    public void applyProductCouponDiscount(int discount) {
        this.productCouponDiscount = discount;
    }

    public void applyOrderCouponDiscount(int discount) {
        this.orderCouponDiscount = discount;
    }

    public int grossAmount() {
        return unitPrice * quantity;
    }

    public int getLineAmount() {
        return discountedUnitPrice * quantity - productCouponDiscount;
    }

    public int getProductDiscountAmount() {
        return unitDiscountAmount * quantity;
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
                .finalAmount(discountedUnitPrice * quantity - productCouponDiscount - orderCouponDiscount)
                .memberCouponId(memberCouponId)
                .build();
    }
}
