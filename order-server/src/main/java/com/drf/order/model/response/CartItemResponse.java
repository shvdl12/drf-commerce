package com.drf.order.model.response;

import com.drf.order.client.dto.ProductResponse;
import com.drf.order.entity.Cart;

public record CartItemResponse(
        long cartId,
        long productId,
        String productName,
        int price,
        int quantity,
        int subtotal,
        String status,
        boolean outOfStock,
        boolean insufficientStock
) {
    public static CartItemResponse of(Cart cart, ProductResponse product) {
        int stock = product.stock();
        int qty = cart.getQuantity();
        return new CartItemResponse(
                cart.getId(),
                cart.getProductId(),
                product.name(),
                product.price(),
                qty,
                product.price() * qty,
                product.status(),
                stock == 0,
                stock > 0 && stock < qty
        );
    }
}
