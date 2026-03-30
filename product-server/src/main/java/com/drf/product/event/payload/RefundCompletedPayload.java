package com.drf.product.event.payload;

public record RefundCompletedPayload(long productId, int quantity) {
}
