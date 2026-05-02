package com.drf.product.model.response;

import com.drf.common.model.Money;
import com.drf.product.entity.Product;
import com.drf.product.entity.ProductStatus;
import lombok.Builder;

import java.util.List;

@Builder
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
        ProductStatus status
) {

    public static InternalProductResponse from(Product product, Money discountAmount, Money discountedPrice, List<Long> categoryPath) {
        return InternalProductResponse.builder()
                .id(product.getId())
                .categoryId(product.getCategory().getId())
                .categoryPath(categoryPath)
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice().toLong())
                .discountRate(product.getDiscountRate())
                .discountAmount(discountAmount.toLong())
                .discountedPrice(discountedPrice.toLong())
                .status(product.getStatus())
                .build();
    }
}
