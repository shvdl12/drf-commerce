package com.drf.order.client.dto.request;

import java.util.List;

public record StockBatchReleaseRequest(List<StockBatchReleaseItem> items) {
    public record StockBatchReleaseItem(long productId, int quantity) {
    }
}
