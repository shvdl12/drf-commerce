package com.drf.product.model.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record StockReserveRequest(
        @Min(1)
        @NotNull
        Integer quantity
) {
}
