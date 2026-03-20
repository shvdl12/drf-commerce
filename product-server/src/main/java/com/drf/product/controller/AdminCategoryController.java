package com.drf.product.controller;

import com.drf.common.model.CommonResponse;
import com.drf.product.model.request.CategoryCreateRequest;
import com.drf.product.model.request.CategoryUpdateRequest;
import com.drf.product.model.response.CategoryCreateResponse;
import com.drf.product.model.response.CategoryTreeResponse;
import com.drf.product.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CategoryService categoryService;

    @PostMapping("/categories")
    public ResponseEntity<CommonResponse<CategoryCreateResponse>> createCategory(
            @Valid @RequestBody CategoryCreateRequest request) {
        Long created = categoryService.createCategory(request);
        return ResponseEntity.ok(CommonResponse.success(new CategoryCreateResponse(created)));
    }

    @GetMapping("/categories")
    public ResponseEntity<CommonResponse<List<CategoryTreeResponse>>> getCategories() {
        return ResponseEntity.ok(CommonResponse.success(categoryService.getCategories()));
    }

    @PostMapping("/categories/{id}")
    public ResponseEntity<Void> updateCategory(
            @PathVariable long id, @Valid @RequestBody CategoryUpdateRequest request) {
        categoryService.updateCategory(id, request);
        return ResponseEntity.noContent().build();
    }
}
