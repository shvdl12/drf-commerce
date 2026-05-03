package com.drf.order.model.response;

import com.drf.common.model.Money;
import com.drf.order.client.dto.response.InternalProductResponse;

public record CheckoutAvailableItem(
        long productId,
        String name,
        long price,
        long discountedPrice,
        int quantity,
        long subtotal
) {

    public static CheckoutAvailableItem of(InternalProductResponse product, int quantity, Money subtotal) {
        return new CheckoutAvailableItem(
                product.id(),
                product.name(),
                product.price(),
                product.discountedPrice(),
                quantity,
                subtotal.toLong()
        );
    }
}
