package com.drf.order.model.response;

import com.drf.common.model.Money;
import lombok.Builder;

import java.util.List;

@Builder
public record CheckoutResponse(
        List<CheckoutAvailableItem> availableItems,
        List<CheckoutUnavailableItem> unavailableItems,
        long orderAmount,
        long deliveryFee,
        long totalAmount
) {
    public static CheckoutResponse of(List<CheckoutAvailableItem> availableItems,
                                      List<CheckoutUnavailableItem> unavailableItems,
                                      Money orderAmount,
                                      Money deliveryFee,
                                      Money totalAmount) {
        return CheckoutResponse.builder()
                .availableItems(availableItems)
                .unavailableItems(unavailableItems)
                .orderAmount(orderAmount.toLong())
                .deliveryFee(deliveryFee.toLong())
                .totalAmount(totalAmount.toLong())
                .build();
    }
}
