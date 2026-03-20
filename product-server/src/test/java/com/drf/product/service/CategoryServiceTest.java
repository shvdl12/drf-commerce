package com.drf.product.service;

import com.drf.common.exception.BusinessException;
import com.drf.product.common.exception.ErrorCode;
import com.drf.product.entity.Category;
import com.drf.product.model.request.CategoryCreateRequest;
import com.drf.product.model.request.CategoryUpdateRequest;
import com.drf.product.model.response.CategoryTreeResponse;
import com.drf.product.repository.CategoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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

    @Nested
    @DisplayName("카테고리 계층 전체 조회")
    class GetCategories {

        @Test
        @DisplayName("빈 목록 반환")
        void getCategories_empty() {
            // given
            given(categoryRepository.findAll()).willReturn(List.of());

            // when
            List<CategoryTreeResponse> result = categoryService.getCategories();

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("단일 루트 카테고리 - children 비어있음")
        void getCategories_singleRoot() {
            // given
            Category root = Category.builder().id(1L).name("전자제품").build();
            given(categoryRepository.findAll()).willReturn(List.of(root));

            // when
            List<CategoryTreeResponse> result = categoryService.getCategories();

            // then
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().id()).isEqualTo(1L);
            assertThat(result.getFirst().name()).isEqualTo("전자제품");
            assertThat(result.getFirst().children()).isEmpty();
        }

        @Test
        @DisplayName("계층 구조 트리 빌드 - 루트 → 자식 → 손자")
        void getCategories_hierarchy() {
            // given
            Category root = Category.builder().id(1L).name("전자제품").build();
            Category child = Category.builder().id(2L).name("스마트폰").parent(root).build();
            Category grandchild = Category.builder().id(3L).name("안드로이드").parent(child).build();
            given(categoryRepository.findAll()).willReturn(List.of(root, child, grandchild));

            // when
            List<CategoryTreeResponse> result = categoryService.getCategories();

            // then
            assertThat(result).hasSize(1);
            CategoryTreeResponse rootNode = result.getFirst();
            assertThat(rootNode.name()).isEqualTo("전자제품");
            assertThat(rootNode.children()).hasSize(1);
            CategoryTreeResponse childNode = rootNode.children().getFirst();
            assertThat(childNode.name()).isEqualTo("스마트폰");
            assertThat(childNode.children()).hasSize(1);
            assertThat(childNode.children().getFirst().name()).isEqualTo("안드로이드");
        }
    }

    @Nested
    @DisplayName("카테고리 수정")
    class UpdateCategory {

        @Test
        @DisplayName("루트 카테고리 이름 수정 성공")
        void updateCategory_success_root() {
            // given
            Category category = Category.builder().id(1L).name("전자제품").build();
            given(categoryRepository.findById(1L)).willReturn(Optional.of(category));
            given(categoryRepository.existsByParentIdAndName(null, "가전제품")).willReturn(false);

            // when
            categoryService.updateCategory(1L, new CategoryUpdateRequest("가전제품"));

            // then
            assertThat(category.getName()).isEqualTo("가전제품");
        }

        @Test
        @DisplayName("하위 카테고리 이름 수정 성공")
        void updateCategory_success_child() {
            // given
            Category parent = Category.builder().id(1L).name("전자제품").build();
            Category category = Category.builder().id(2L).name("스마트폰").parent(parent).build();
            given(categoryRepository.findById(2L)).willReturn(Optional.of(category));
            given(categoryRepository.existsByParentIdAndName(1L, "태블릿")).willReturn(false);

            // when
            categoryService.updateCategory(2L, new CategoryUpdateRequest("태블릿"));

            // then
            assertThat(category.getName()).isEqualTo("태블릿");
        }

        @Test
        @DisplayName("존재하지 않는 카테고리 수정 시 예외 발생")
        void updateCategory_notFound() {
            // given
            given(categoryRepository.findById(99L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> categoryService.updateCategory(99L, new CategoryUpdateRequest("가전제품")))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CATEGORY_NOT_FOUND);
        }

        @Test
        @DisplayName("동일 계층 내 이름 중복 시 예외 발생")
        void updateCategory_duplicateName() {
            // given
            Category parent = Category.builder().id(1L).name("전자제품").build();
            Category category = Category.builder().id(2L).name("스마트폰").parent(parent).build();
            given(categoryRepository.findById(2L)).willReturn(Optional.of(category));
            given(categoryRepository.existsByParentIdAndName(1L, "태블릿")).willReturn(true);

            // when & then
            assertThatThrownBy(() -> categoryService.updateCategory(2L, new CategoryUpdateRequest("태블릿")))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_CATEGORY_NAME);
        }
    }
}
