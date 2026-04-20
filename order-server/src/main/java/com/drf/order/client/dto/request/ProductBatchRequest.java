package com.drf.order.client.dto.request;

import java.util.List;

public record ProductBatchRequest(
        List<Long> ids
) {
}
