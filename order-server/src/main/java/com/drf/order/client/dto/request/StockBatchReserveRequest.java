package com.drf.order.client.dto.request;

import java.util.List;

public record StockBatchReserveRequest(List<StockBatchReserveItem> items) {
    public record StockBatchReserveItem(long productId, int quantity) {
    }
}
