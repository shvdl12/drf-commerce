package com.drf.order.model.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record OrderCreateRequest(
        @NotEmpty List<Long> cartItemIds,
        @NotNull Long shippingAddressId,
        @NotBlank String paymentMethodId,
        @Min(0) long expectedAmount
) {
}
