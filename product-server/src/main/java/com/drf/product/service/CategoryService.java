package com.drf.product.service;

import com.drf.common.exception.BusinessException;
import com.drf.product.common.exception.ErrorCode;
import com.drf.product.entity.Category;
import com.drf.product.model.request.CategoryCreateRequest;
import com.drf.product.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}