package com.drf.product.event.payload;

public record PaymentCompletedPayload(long productId, int quantity) {
}
