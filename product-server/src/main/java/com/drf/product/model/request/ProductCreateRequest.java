package com.drf.product.model.request;

import jakarta.validation.constraints.*;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ProductCreateRequest(
        @NotNull
        Long categoryId,

        @NotBlank
        @Size(max = 100)
        String name,

        @Min(0)
        @NotNull
        Long price,

        @NotBlank
        String description,

        @Min(0)
        @Max(100)
        Integer discountRate,

        LocalDateTime saleStartAt,

        LocalDateTime saleEndAt
) {
}
