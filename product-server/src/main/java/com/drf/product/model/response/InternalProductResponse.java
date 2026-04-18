package com.drf.product.model.response;

import com.drf.product.entity.Product;
import com.drf.product.entity.ProductStatus;
import lombok.Builder;

import java.util.List;

@Builder
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
        ProductStatus status,
        int stock
) {

    public static InternalProductResponse from(Product product, int discountAmount, int discountedPrice, List<Long> categoryPath) {
        return InternalProductResponse.builder()
                .id(product.getId())
                .categoryId(product.getCategory().getId())
                .categoryPath(categoryPath)
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .discountRate(product.getDiscountRate())
                .discountAmount(discountAmount)
                .discountedPrice(discountedPrice)
                .status(product.getStatus())
                .stock(product.getStock().getStock())
                .build();
    }
}
