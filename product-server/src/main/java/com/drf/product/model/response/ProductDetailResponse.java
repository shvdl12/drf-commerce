package com.drf.product.model.response;

import com.drf.product.entity.Product;
import com.drf.product.entity.ProductStatus;

import java.time.LocalDateTime;

public record ProductDetailResponse(
        long id,
        String categoryName,
        String name,
        long price,
        String description,
        ProductStatus status,
        int discountRate,
        LocalDateTime saleStartAt,
        LocalDateTime saleEndAt
) {
    public static ProductDetailResponse from(Product product) {
        return new ProductDetailResponse(
                product.getId(),
                product.getCategory().getName(),
                product.getName(),
                product.getPrice().toLong(),
                product.getDescription(),
                product.getStatus(),
                product.getDiscountRate(),
                product.getSaleStartAt(),
                product.getSaleEndAt()
        );
    }
}
