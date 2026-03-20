package com.drf.product.service;

import com.drf.common.exception.BusinessException;
import com.drf.product.common.exception.ErrorCode;
import com.drf.product.entity.Category;
import com.drf.product.model.request.CategoryCreateRequest;
import com.drf.product.repository.CategoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
public class CategoryServiceTest {

    @InjectMocks
    private CategoryService categoryService;

    @Mock
    private CategoryRepository categoryRepository;


    @Nested
    @DisplayName("카테고리 등록")
    class CreateCategory {

        @Test
        @DisplayName("최상위 카테고리 등록 성공")
        void createCategory_success_root() {
            // given
            CategoryCreateRequest request = new CategoryCreateRequest(null, "전자제품");
            given(categoryRepository.existsByParentIdIsNullAndName("전자제품")).willReturn(false);

            // when
            categoryService.createCategory(request);

            // then
            then(categoryRepository).should().save(any(Category.class));
        }

        @Test
        @DisplayName("하위 카테고리 등록 성공")
        void createCategory_success_child() {
            // given
            CategoryCreateRequest request = new CategoryCreateRequest(1L, "스마트폰");
            given(categoryRepository.existsById(1L)).willReturn(true);
            given(categoryRepository.existsByParentIdAndName(1L, "스마트폰")).willReturn(false);
            Category parent = Category.builder().id(1L).name("전자제품").build();
            given(categoryRepository.getReferenceById(1L)).willReturn(parent);

            // when
            categoryService.createCategory(request);

            // then
            then(categoryRepository).should().save(any(Category.class));
        }

        @Test
        @DisplayName("존재하지 않는 부모 카테고리로 등록 시 예외 발생")
        void createCategory_parentNotFound() {
            // given
            CategoryCreateRequest request = new CategoryCreateRequest(99L, "스마트폰");
            given(categoryRepository.existsById(99L)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> categoryService.createCategory(request))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CATEGORY_NOT_FOUND);

            then(categoryRepository).should(org.mockito.Mockito.never()).save(any());
        }

        @Test
        @DisplayName("최상위 카테고리 이름 중복 시 예외 발생")
        void createCategory_duplicateName_root() {
            // given
            CategoryCreateRequest request = new CategoryCreateRequest(null, "전자제품");
            given(categoryRepository.existsByParentIdIsNullAndName("전자제품")).willReturn(true);

            // when & then
            assertThatThrownBy(() -> categoryService.createCategory(request))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_CATEGORY_NAME);

            then(categoryRepository).should(org.mockito.Mockito.never()).save(any());
        }

        @Test
        @DisplayName("동일 부모 하위 카테고리 이름 중복 시 예외 발생")
        void createCategory_duplicateName_child() {
            // given
            CategoryCreateRequest request = new CategoryCreateRequest(1L, "스마트폰");
            given(categoryRepository.existsById(1L)).willReturn(true);
            given(categoryRepository.existsByParentIdAndName(1L, "스마트폰")).willReturn(true);
            Category parent = Category.builder().id(1L).name("전자제품").build();
            given(categoryRepository.getReferenceById(1L)).willReturn(parent);

            // when & then
            assertThatThrownBy(() -> categoryService.createCategory(request))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_CATEGORY_NAME);

            then(categoryRepository).should(org.mockito.Mockito.never()).save(any());
        }
    }
}
