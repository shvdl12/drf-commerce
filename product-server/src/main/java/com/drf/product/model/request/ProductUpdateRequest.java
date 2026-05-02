package com.drf.product.model.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ProductUpdateRequest(
        Long categoryId,

        @Size(min = 1, max = 100)
        String name,

        @Min(0)
        Long price,

        @Size(min = 1)
        String description,

        @Min(0)
        @Max(100)
        Integer discountRate,

        LocalDateTime saleStartAt,

        LocalDateTime saleEndAt
) {
}
