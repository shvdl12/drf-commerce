package com.drf.product.model.response;

public record StockReleaseResponse(
        long productId,
        int remainingStock
) {
}
