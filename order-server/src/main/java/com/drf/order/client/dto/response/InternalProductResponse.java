package com.drf.order.client.dto.response;

import java.util.List;

public record InternalProductResponse(
        long id,
        String name,
        String description,
        int price,
        int discountRate,
        int discountAmount,
        int discountedPrice,
        long categoryId,
        List<Long> categoryPath,
        String status,
        int stock
) {
}