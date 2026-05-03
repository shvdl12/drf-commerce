package com.drf.order.client.dto.response;

import com.drf.order.model.type.ProductStatus;

import java.util.List;

public record InternalProductResponse(
        long id,
        String name,
        String description,
        long price,
        int discountRate,
        long discountAmount,
        long discountedPrice,
        long categoryId,
        List<Long> categoryPath,
        ProductStatus status,
        int stock
) {
}