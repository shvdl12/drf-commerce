package com.drf.product.model.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ProductBatchRequest(
        @NotEmpty List<Long> ids
) {
}
