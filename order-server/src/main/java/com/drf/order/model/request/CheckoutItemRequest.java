package com.drf.order.model.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CheckoutItemRequest(
        @NotNull Long productId,
        @Min(1) int quantity
) {
}
