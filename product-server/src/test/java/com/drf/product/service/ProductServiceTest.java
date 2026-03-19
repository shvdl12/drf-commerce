package com.drf.product.service;

import com.drf.common.exception.BusinessException;
import com.drf.product.common.exception.ErrorCode;
import com.drf.product.entity.Category;
import com.drf.product.entity.Product;
import com.drf.product.entity.ProductStatus;
import com.drf.product.entity.ProductStock;
import com.drf.product.event.ProductCreatedEvent;
import com.drf.product.event.ProductDeletedEvent;
import com.drf.product.event.ProductUpdatedEvent;
import com.drf.product.model.request.ProductCreateRequest;
import com.drf.product.model.request.ProductUpdateRequest;
import com.drf.product.repository.CategoryRepository;
import com.drf.product.repository.ProductRepository;
import com.drf.product.repository.ProductStockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.Optional;

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

    @Mock
    private ProductStockRepository productStockRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;


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
                    .stock(100)
                    .price(10000)
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
                    .price(request.price())
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
            then(productStockRepository).should().save(any(ProductStock.class));
            then(eventPublisher).should().publishEvent(any(ProductCreatedEvent.class));
        }

        @Test
        @DisplayName("세일 시작과 종료 중 하나만 존재할 경우 예외 발생")
        void createProduct_incompleteSaleDate() {
            // given
            request = ProductCreateRequest.builder()
                    .categoryId(1L)
                    .name("상품명")
                    .stock(100)
                    .price(10000)
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
                    .stock(100)
                    .price(10000)
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
            then(productStockRepository).should(never()).save(any());
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
                    .price(10000)
                    .build();
        }

        @Test
        @DisplayName("전체 필드 수정 성공")
        void updateProduct_success() {
            // given
            request = ProductUpdateRequest.builder()
                    .categoryId(2L)
                    .name("수정된 상품명")
                    .price(20000)
                    .stock(50)
                    .build();

            Category newCategory = Category.builder().name("새 카테고리").build();
            ProductStock productStock = ProductStock.create(product, 100);

            given(productRepository.findById(1L)).willReturn(Optional.of(product));
            given(categoryRepository.findById(2L)).willReturn(Optional.of(newCategory));
            given(productStockRepository.findById(1L)).willReturn(Optional.of(productStock));

            // when
            productService.updateProduct(1L, request);

            // then
            then(productRepository).should().findById(1L);
            then(categoryRepository).should().findById(2L);
            then(productStockRepository).should().findById(1L);
            then(eventPublisher).should().publishEvent(any(ProductUpdatedEvent.class));
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

            // then
            then(productStockRepository).should(never()).findById(any());
            then(eventPublisher).should(never()).publishEvent(any());
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

            then(productStockRepository).should(never()).findById(any());
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
    @DisplayName("상품 삭제")
    class DeleteProduct {
        private Product product;

        @BeforeEach
        void setUp() {
            product = Product.builder()
                    .id(1L)
                    .name("상품명")
                    .price(10000)
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
            then(eventPublisher).should().publishEvent(any(ProductDeletedEvent.class));
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
