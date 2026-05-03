package com.drf.order.model.response;

public record CheckoutUnavailableItem(
        long productId,
        String name,
        UnavailableReason reason
) {
    public enum UnavailableReason {
        NOT_ON_SALE, OUT_OF_STOCK, INSUFFICIENT_STOCK
    }

    public static CheckoutUnavailableItem of(long productId, String name, UnavailableReason reason) {
        return new CheckoutUnavailableItem(productId, name, reason);
    }
}
