package com.drf.product.model.response;

import com.drf.product.entity.Product;
import com.drf.product.entity.ProductStatus;

import java.time.LocalDateTime;

public record ProductListResponse(
        long id,
        String categoryName,
        String name,
        long price,
        ProductStatus status,
        int discountRate,
        LocalDateTime createdAt
) {
    public static ProductListResponse from(Product product) {
        return new ProductListResponse(
                product.getId(),
                product.getCategory().getName(),
                product.getName(),
                product.getPrice().toLong(),
                product.getStatus(),
                product.getDiscountRate(),
                product.getCreatedAt()
        );
    }
}
