package com.drf.product.controller;

import com.drf.common.model.CommonResponse;
import com.drf.product.model.request.CategoryCreateRequest;
import com.drf.product.model.response.CategoryCreateResponse;
import com.drf.product.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

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
}
