package com.drf.product.service;

import com.drf.common.exception.BusinessException;
import com.drf.common.model.Money;
import com.drf.product.common.exception.ErrorCode;
import com.drf.product.entity.Category;
import com.drf.product.entity.Product;
import com.drf.product.entity.ProductStatus;
import com.drf.product.model.request.ProductCreateRequest;
import com.drf.product.model.request.ProductUpdateRequest;
import com.drf.product.model.response.ProductDetailResponse;
import com.drf.product.model.response.ProductListResponse;
import com.drf.product.repository.CategoryRepository;
import com.drf.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
public class ProductServiceTest {

    @InjectMocks
    private ProductService productService;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;


    @Nested
    @DisplayName("상품 등록")
    class CreateProduct {
        private ProductCreateRequest request;
        private Category category;

        @BeforeEach
        void setUp() {
            request = ProductCreateRequest.builder()
                    .categoryId(1L)
                    .name("상품명")
                    .price(10000L)
                    .description("상품 설명")
                    .discountRate(10)
                    .saleStartAt(LocalDateTime.of(2026, 3, 1, 0, 0, 0))
                    .saleEndAt(LocalDateTime.of(2026, 4, 1, 0, 0, 0))
                    .build();

            category = Category.builder()
                    .name("카테고리")
                    .build();
        }

        @Test
        @DisplayName("등록 성공")
        void createProduct_success() {
            // given
            given(categoryRepository.findById(request.categoryId()))
                    .willReturn(Optional.of(category));

            Product savedProduct = Product.builder()
                    .id(1L)
                    .category(category)
                    .name(request.name())
                    .price(Money.of(request.price()))
                    .description(request.description())
                    .status(ProductStatus.READY)
                    .discountRate(request.discountRate())
                    .saleStartAt(request.saleStartAt())
                    .saleEndAt(request.saleEndAt())
                    .build();

            given(productRepository.save(any(Product.class)))
                    .willReturn(savedProduct);

            // when
            productService.createProduct(request);

            // then
            then(categoryRepository).should().findById(request.categoryId());
            then(productRepository).should().save(any(Product.class));
        }

        @Test
        @DisplayName("세일 시작과 종료 중 하나만 존재할 경우 예외 발생")
        void createProduct_incompleteSaleDate() {
            // given
            request = ProductCreateRequest.builder()
                    .categoryId(1L)
                    .name("상품명")
                    .price(10000L)
                    .description("상품 설명")
                    .discountRate(10)
                    .saleStartAt(LocalDateTime.of(2026, 3, 1, 0, 0, 0))
                    .build();

            // when & then
            assertThatThrownBy(() -> productService.createProduct(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INCOMPLETE_SALE_DATE);
        }

        @Test
        @DisplayName("세일 종료 시간이 시작 시간보다 과거일 경우 예외 발생")
        void createProduct_invalidSaleDateRange() {
            // given
            request = ProductCreateRequest.builder()
                    .categoryId(1L)
                    .name("상품명")
                    .price(10000L)
                    .description("상품 설명")
                    .discountRate(10)
                    .saleStartAt(LocalDateTime.of(2026, 3, 1, 0, 0, 0))
                    .saleEndAt(LocalDateTime.of(2000, 4, 1, 0, 0, 0))
                    .build();

            // when & then
            assertThatThrownBy(() -> productService.createProduct(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_SALE_DATE_RANGE);
        }

        @Test
        @DisplayName("존재하지 않는 카테고리로 등록 시 실패")
        void createProduct_categoryNotFound() {
            // given
            given(categoryRepository.findById(request.categoryId()))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> productService.createProduct(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.CATEGORY_NOT_FOUND);

            then(productRepository).should(never()).save(any());
        }
    }

    @Nested
    @DisplayName("상품 수정")
    class UpdateProduct {
        private ProductUpdateRequest request;
        private Product product;
        private Category category;

        @BeforeEach
        void setUp() {
            category = Category.builder()
                    .name("카테고리")
                    .build();

            product = Product.builder()
                    .id(1L)
                    .category(category)
                    .name("상품명")
                    .price(Money.of(10000))
                    .build();
        }

        @Test
        @DisplayName("전체 필드 수정 성공")
        void updateProduct_success() {
            // given
            request = ProductUpdateRequest.builder()
                    .categoryId(2L)
                    .name("수정된 상품명")
                    .price(20000L)
                    .build();

            Category newCategory = Category.builder().name("새 카테고리").build();

            given(productRepository.findById(1L)).willReturn(Optional.of(product));
            given(categoryRepository.findById(2L)).willReturn(Optional.of(newCategory));

            // when
            productService.updateProduct(1L, request);

            // then
            then(productRepository).should().findById(1L);
            then(categoryRepository).should().findById(2L);
        }

        @Test
        @DisplayName("재고 없이 수정 시 재고 관련 로직 호출 안 함")
        void updateProduct_withoutStock() {
            // given
            request = ProductUpdateRequest.builder()
                    .name("수정된 상품명")
                    .build();

            given(productRepository.findById(1L)).willReturn(Optional.of(product));

            // when
            productService.updateProduct(1L, request);
        }

        @Test
        @DisplayName("존재하지 않는 상품 수정 시 실패")
        void updateProduct_productNotFound() {
            // given
            request = ProductUpdateRequest.builder()
                    .name("수정된 상품명")
                    .build();

            given(productRepository.findById(1L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> productService.updateProduct(1L, request))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("존재하지 않는 카테고리로 수정 시 실패")
        void updateProduct_categoryNotFound() {
            // given
            request = ProductUpdateRequest.builder()
                    .categoryId(99L)
                    .build();

            given(productRepository.findById(1L)).willReturn(Optional.of(product));
            given(categoryRepository.findById(99L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> productService.updateProduct(1L, request))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("상품 상세 조회")
    class GetProduct {
        private Category category;
        private Product product;

        @BeforeEach
        void setUp() {
            category = Category.builder()
                    .name("카테고리")
                    .build();

            product = Product.builder()
                    .id(1L)
                    .category(category)
                    .name("상품명")
                    .price(Money.of(10000))
                    .description("상품 설명")
                    .status(ProductStatus.READY)
                    .discountRate(10)
                    .build();
        }

        @Test
        @DisplayName("상품이 존재하면 상세 정보를 반환한다")
        void success() {
            // given
            given(productRepository.findById(1L)).willReturn(Optional.of(product));

            // when
            ProductDetailResponse response = productService.getProduct(1L);

            // then
            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.categoryName()).isEqualTo("카테고리");
            assertThat(response.name()).isEqualTo("상품명");
            assertThat(response.price()).isEqualTo(10000);
            assertThat(response.discountRate()).isEqualTo(10);
        }

        @Test
        @DisplayName("상품이 존재하지 않으면 예외를 던진다")
        void fail_productNotFound() {
            // given
            given(productRepository.findById(1L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> productService.getProduct(1L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("상품명 검색")
    class SearchProductsByName {
        private Category category;
        private Product product;

        @BeforeEach
        void setUp() {
            category = Category.builder().name("카테고리").build();
            product = Product.builder()
                    .id(1L)
                    .category(category)
                    .name("상품명")
                    .price(Money.of(10000))
                    .status(ProductStatus.READY)
                    .discountRate(10)
                    .build();
        }

        @Test
        @DisplayName("검색어를 포함하는 상품 목록을 페이지로 반환한다")
        void success() {
            // given
            Pageable pageable = PageRequest.of(0, 20);
            given(productRepository.findByNameContainingAndStatusNot("상품", ProductStatus.DELETED, pageable))
                    .willReturn(new PageImpl<>(List.of(product), pageable, 1));

            // when
            Page<ProductListResponse> result = productService.searchProductsByName("상품", pageable);

            // then
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).id()).isEqualTo(1L);
            assertThat(result.getContent().get(0).name()).isEqualTo("상품명");
        }
    }

    @Nested
    @DisplayName("카테고리별 상품 조회")
    class GetProductsByCategory {
        private Category category;
        private Product product;

        @BeforeEach
        void setUp() {
            category = Category.builder().name("카테고리").build();
            product = Product.builder()
                    .id(1L)
                    .category(category)
                    .name("상품명")
                    .price(Money.of(10000))
                    .status(ProductStatus.READY)
                    .discountRate(10)
                    .build();
        }

        @Test
        @DisplayName("카테고리에 속한 상품 목록을 페이지로 반환한다 (하위 카테고리 포함)")
        void success() {
            // given
            Pageable pageable = PageRequest.of(0, 20);
            given(categoryRepository.findById(1L)).willReturn(Optional.of(category));
            given(categoryRepository.findByParentId(1L)).willReturn(List.of());
            given(productRepository.findByCategoryIdInAndStatusNot(Set.of(1L), ProductStatus.DELETED, pageable))
                    .willReturn(new PageImpl<>(List.of(product), pageable, 1));

            // when
            Page<ProductListResponse> result = productService.getProductsByCategory(1L, pageable);

            // then
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).id()).isEqualTo(1L);
            assertThat(result.getContent().get(0).categoryName()).isEqualTo("카테고리");
        }

        @Test
        @DisplayName("상위 카테고리로 조회하면 하위 카테고리의 상품도 반환한다")
        void success_withDescendantCategories() {
            // given
            // 카테고리 구조: 상위(1) -> 하위(2) -> 최하위(3)
            Category parent = Category.builder().id(1L).name("상위카테고리").build();
            Category child = Category.builder().id(2L).name("하위카테고리").parent(parent).build();
            Category grandchild = Category.builder().id(3L).name("최하위카테고리").parent(child).build();

            Product childProduct = Product.builder()
                    .id(2L).category(grandchild).name("최하위상품").price(Money.of(5000))
                    .status(ProductStatus.READY).discountRate(0).build();

            Pageable pageable = PageRequest.of(0, 20);
            given(categoryRepository.findById(1L)).willReturn(Optional.of(parent));
            given(categoryRepository.findByParentId(1L)).willReturn(List.of(child));
            given(categoryRepository.findByParentId(2L)).willReturn(List.of(grandchild));
            given(categoryRepository.findByParentId(3L)).willReturn(List.of());
            given(productRepository.findByCategoryIdInAndStatusNot(Set.of(1L, 2L, 3L), ProductStatus.DELETED, pageable))
                    .willReturn(new PageImpl<>(List.of(childProduct), pageable, 1));

            // when
            Page<ProductListResponse> result = productService.getProductsByCategory(1L, pageable);

            // then
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).categoryName()).isEqualTo("최하위카테고리");
        }

        @Test
        @DisplayName("존재하지 않는 카테고리 조회 시 예외를 던진다")
        void fail_categoryNotFound() {
            // given
            Pageable pageable = PageRequest.of(0, 20);
            given(categoryRepository.findById(99L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> productService.getProductsByCategory(99L, pageable))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CATEGORY_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("상품 삭제")
    class DeleteProduct {
        private Product product;

        @BeforeEach
        void setUp() {
            product = Product.builder()
                    .id(1L)
                    .name("상품명")
                    .price(Money.of(10000))
                    .build();
        }

        @Test
        @DisplayName("상품이 존재하면 상품을 삭제하고 이벤트를 발행한다")
        void success() {
            // given
            given(productRepository.findById(1L)).willReturn(Optional.of(product));

            // when
            productService.deleteProduct(1L);

            // then
            assertThat(product.getStatus()).isEqualTo(ProductStatus.DELETED);
            assertThat(product.getDeletedAt()).isNotNull();
        }

        @Test
        @DisplayName("상품이 존재하지 않으면 예외를 던진다")
        void fail_productNotFound() {
            // given
            given(productRepository.findById(1L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> productService.deleteProduct(1L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
        }
    }
}
