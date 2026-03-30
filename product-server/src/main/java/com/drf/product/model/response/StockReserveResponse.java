package com.drf.product.model.response;

public record StockReserveResponse(
        long productId,
        int remainingStock
) {
}
