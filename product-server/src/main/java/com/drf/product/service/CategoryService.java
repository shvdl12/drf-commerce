package com.drf.product.service;

import com.drf.common.exception.BusinessException;
import com.drf.product.common.exception.ErrorCode;
import com.drf.product.entity.Category;
import com.drf.product.model.request.CategoryCreateRequest;
import com.drf.product.model.request.CategoryUpdateRequest;
import com.drf.product.model.response.CategoryTreeResponse;
import com.drf.product.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;


    @Transactional
    public Long createCategory(CategoryCreateRequest request) {
        Category parent = null;

        if (request.parentId() != null) {
            if (!categoryRepository.existsById(request.parentId())) {
                throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
            }

            parent = categoryRepository.getReferenceById(request.parentId());

            if (categoryRepository.existsByParentIdAndName(request.parentId(), request.name())) {
                throw new BusinessException(ErrorCode.DUPLICATE_CATEGORY_NAME);
            }

        } else {
            if (categoryRepository.existsByParentIdIsNullAndName(request.name())) {
                throw new BusinessException(ErrorCode.DUPLICATE_CATEGORY_NAME);
            }
        }

        Category category = Category.create(parent, request.name());
        categoryRepository.save(category);

        return category.getId();
    }

    @Transactional(readOnly = true)
    public List<CategoryTreeResponse> getCategories() {
        List<Category> categories = categoryRepository.findAll();

        Map<Long, CategoryTreeResponse> nodeMap = categories.stream()
                .collect(Collectors.toMap(Category::getId, CategoryTreeResponse::from));

        List<CategoryTreeResponse> roots = new ArrayList<>();

        for (Category c : categories) {
            if (c.getParent() == null) {
                // 부모가 존재하지 않을 경우 roots 삽입
                roots.add(nodeMap.get(c.getId()));
            } else {
                // 부모가 존재하는 경우 부모의 children 삽입
                nodeMap.get(c.getParent().getId()).addChild(nodeMap.get(c.getId()));
            }
        }

        return roots;
    }

    @Transactional
    public void updateCategory(Long categoryId, CategoryUpdateRequest request) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));

        Long parentId = category.getParent() != null ? category.getParent().getId() : null;

        if (categoryRepository.existsByParentIdAndName(parentId, request.name())) {
            throw new BusinessException(ErrorCode.DUPLICATE_CATEGORY_NAME);
        }

        category.updateName(request.name());
    }
}