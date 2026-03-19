package com.drf.product.service;

import com.drf.common.exception.BusinessException;
import com.drf.product.common.exception.ErrorCode;
import com.drf.product.entity.Category;
import com.drf.product.entity.Product;
import com.drf.product.entity.ProductStock;
import com.drf.product.event.ProductCreatedEvent;
import com.drf.product.event.ProductDeletedEvent;
import com.drf.product.event.ProductUpdatedEvent;
import com.drf.product.model.request.ProductCreateRequest;
import com.drf.product.model.request.ProductUpdateRequest;
import com.drf.product.repository.CategoryRepository;
import com.drf.product.repository.ProductRepository;
import com.drf.product.repository.ProductStockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductStockRepository productStockRepository;
    private final ApplicationEventPublisher eventPublisher;


    @Transactional
    public Long createProduct(ProductCreateRequest request) {
        validateDateRange(request.saleStartAt(), request.saleEndAt());

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));

        Product product = Product.create(category, request.name(), request.price(), request.description(),
                request.discountRate(), request.saleStartAt(), request.saleEndAt());

        Product savedProduct = productRepository.save(product);

        ProductStock productStock = ProductStock.create(savedProduct, request.stock());
        productStockRepository.save(productStock);

        eventPublisher.publishEvent(new ProductCreatedEvent(savedProduct.getId(), request.stock()));

        return savedProduct.getId();
    }

    @Transactional
    public void updateProduct(long id, ProductUpdateRequest request) {
        validateDateRange(request.saleStartAt(), request.saleEndAt());

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        if (request.stock() != null) {
            ProductStock productStock = productStockRepository.findById(id)
                    .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

            productStock.updateStock(request.stock());
            eventPublisher.publishEvent(new ProductUpdatedEvent(product.getId(), request.stock()));
        }

        Category category = null;
        if (request.categoryId() != null) {
            category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
        }

        product.updateProduct(
                category, request.name(), request.price(), request.description(),
                request.discountRate(), request.saleStartAt(), request.saleEndAt()
        );
    }

    @Transactional
    public void deleteProduct(long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        product.delete();

        eventPublisher.publishEvent(new ProductDeletedEvent(id));
    }

    private void validateDateRange(LocalDateTime startAt, LocalDateTime endAt) {
        // 시작, 종료 기간 둘 중 하나만 있는 경우
        if ((startAt == null) != (endAt == null)) {
            throw new BusinessException(ErrorCode.INCOMPLETE_SALE_DATE);
        }
        // 종료 시간이 시작 시간보다 과거인 경우
        if (startAt != null && !endAt.isAfter(startAt)) {
            throw new BusinessException(ErrorCode.INVALID_SALE_DATE_RANGE);
        }
    }
}
