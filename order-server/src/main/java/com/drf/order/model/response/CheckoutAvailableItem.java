package com.drf.order.model.response;

public record CheckoutAvailableItem(
        long productId,
        String name,
        int price,
        int discountedPrice,
        int quantity,
        int subtotal
) {
}
