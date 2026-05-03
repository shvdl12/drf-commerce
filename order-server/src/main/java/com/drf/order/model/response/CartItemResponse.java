package com.drf.order.model.response;

import com.drf.order.client.dto.response.ProductResponse;
import com.drf.order.entity.CartItem;

public record CartItemResponse(
        long cartItemId,
        long productId,
        String productName,
        long price,
        int quantity,
        long subtotal,
        String status,
        boolean outOfStock,
        boolean insufficientStock
) {
    public static CartItemResponse of(CartItem item, ProductResponse product) {
        int stock = product.stock();
        int qty = item.getQuantity();
        return new CartItemResponse(
                item.getId(),
                item.getProductId(),
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
