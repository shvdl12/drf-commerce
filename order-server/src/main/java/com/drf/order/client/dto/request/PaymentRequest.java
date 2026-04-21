package com.drf.order.client.dto.request;

public record PaymentRequest(long orderId, int amount, String paymentMethodId) {
}
