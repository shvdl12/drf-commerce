package com.drf.inventory.model.response;

public record StockResponse(
        Long productId,
        long stock
) {
}
