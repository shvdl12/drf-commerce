package com.drf.order.model.response;

import java.util.List;

public record CheckoutResponse(
        List<CheckoutAvailableItem> availableItems,
        List<CheckoutUnavailableItem> unavailableItems,
        int itemTotal,
        int shippingFee,
        int totalAmount
) {
}
