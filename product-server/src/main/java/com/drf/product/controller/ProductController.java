package com.drf.product.controller;

import com.drf.common.exception.BusinessException;
import com.drf.common.exception.errorcode.CommonErrorCode;
import com.drf.common.model.CommonResponse;
import com.drf.common.model.PageResponse;
import com.drf.product.model.response.ProductDetailResponse;
import com.drf.product.model.response.ProductListResponse;
import com.drf.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "price");

    @GetMapping("/products/{id}")
    public ResponseEntity<CommonResponse<ProductDetailResponse>> getProduct(@PathVariable long id) {
        return ResponseEntity.ok(CommonResponse.success(productService.getProduct(id)));
    }

    @GetMapping("/internal/products/{id}")
    public ResponseEntity<CommonResponse<ProductDetailResponse>> getProductInternal(@PathVariable long id) {
        return ResponseEntity.ok(CommonResponse.success(productService.getProduct(id)));
    }

    @GetMapping("/products")
    public ResponseEntity<CommonResponse<PageResponse<ProductListResponse>>> searchProductsByName(
            @RequestParam String name,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        validateSortFields(pageable);
        return ResponseEntity.ok(CommonResponse.success(
                PageResponse.from(productService.searchProductsByName(name, pageable))));
    }

    @GetMapping("/products/categories/{categoryId}")
    public ResponseEntity<CommonResponse<PageResponse<ProductListResponse>>> getProductsByCategory(
            @PathVariable long categoryId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        validateSortFields(pageable);
        return ResponseEntity.ok(CommonResponse.success(
                PageResponse.from(productService.getProductsByCategory(categoryId, pageable))));
    }

    private void validateSortFields(Pageable pageable) {
        pageable.getSort().stream()
                .map(Sort.Order::getProperty)
                .filter(property -> !ALLOWED_SORT_FIELDS.contains(property))
                .findFirst()
                .ifPresent(invalid -> {
                    throw new BusinessException(CommonErrorCode.INVALID_SORT_FIELD);
                });
    }
}
