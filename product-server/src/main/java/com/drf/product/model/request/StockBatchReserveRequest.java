package com.drf.product.model.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record StockBatchReserveRequest(
        @NotNull List<StockBatchReserveItem> items
) {
    public record StockBatchReserveItem(
            long productId,
            @Min(1) @NotNull Integer quantity
    ) {
    }
}
