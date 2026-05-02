package com.drf.inventory.model.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record StockBatchLookupRequest(
        @NotEmpty
        List<Long> productIds
) {
}
