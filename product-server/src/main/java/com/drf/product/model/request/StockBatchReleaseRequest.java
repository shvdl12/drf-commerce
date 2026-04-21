package com.drf.product.model.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record StockBatchReleaseRequest(
        @NotNull List<StockBatchReleaseItem> items
) {
    public record StockBatchReleaseItem(
            long productId,
            @Min(1) @NotNull Integer quantity
    ) {
    }
}
