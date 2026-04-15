package com.drf.order.model.request;

import jakarta.validation.constraints.Min;

public record CartUpdateRequest(
        @Min(1) int quantity
) {
}
