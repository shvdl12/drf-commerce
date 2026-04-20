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
import com.drf.product.model.request.ProductBatchRequest;
import com.drf.product.model.request.ProductCreateRequest;
import com.drf.product.model.request.ProductUpdateRequest;
import com.drf.product.model.response.InternalProductResponse;
import com.drf.product.model.response.ProductDetailResponse;
import com.drf.product.model.response.ProductListResponse;
import com.drf.product.repository.CategoryRepository;
import com.drf.product.repository.ProductRepository;
import com.drf.product.repository.ProductStockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
//TODO 퍼사드 적용 및 카테고리 path 생성 로직 개선
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

    @Transactional(readOnly = true)
    public ProductDetailResponse getProduct(long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        ProductStock productStock = productStockRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        return ProductDetailResponse.from(product, productStock);
    }

    private static List<Long> buildCategoryPath(Category category) {
        Deque<Long> path = new ArrayDeque<>();
        Category current = category;
        while (current != null) {
            path.addFirst(current.getId());
            current = current.getParent();
        }
        return List.copyOf(path);
    }

    @Transactional(readOnly = true)
    public List<InternalProductResponse> getProductsByIds(ProductBatchRequest request) {
        List<Product> products = productRepository.findByIdIn(request.ids());

        return products.stream()
                .map(product -> {
                    int discountAmount = calculateDisCountAmount(product.getPrice(), product.getDiscountRate());
                    return InternalProductResponse.from(
                            product,
                            discountAmount,
                            product.getPrice() - discountAmount,
                            buildCategoryPath(product.getCategory())
                    );
                }).toList();
    }

    private int calculateDisCountAmount(int price, int discountRate) {
        return price * discountRate / 100 / 100 * 100;
    }

    @Transactional(readOnly = true)
    public Page<ProductListResponse> searchProductsByName(String name, Pageable pageable) {
        Page<Product> products = productRepository.findByNameContainingAndStatusNot(
                name, ProductStatus.DELETED, pageable);
        return mapToProductListResponse(products);
    }

    @Transactional(readOnly = true)
    public Page<ProductListResponse> getProductsByCategory(long categoryId, Pageable pageable) {
        categoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
        Set<Long> categoryIds = collectDescendantCategoryIds(categoryId);
        Page<Product> products = productRepository.findByCategoryIdInAndStatusNot(
                categoryIds, ProductStatus.DELETED, pageable);
        return mapToProductListResponse(products);
    }

    private Set<Long> collectDescendantCategoryIds(long rootId) {
        Set<Long> ids = new HashSet<>();
        Queue<Long> queue = new ArrayDeque<>();
        queue.add(rootId);
        while (!queue.isEmpty()) {
            Long current = queue.poll();
            ids.add(current);
            categoryRepository.findByParentId(current).stream()
                    .map(Category::getId)
                    .forEach(queue::add);
        }
        return ids;
    }

    private Page<ProductListResponse> mapToProductListResponse(Page<Product> products) {
        List<Long> ids = products.getContent().stream().map(Product::getId).toList();
        Map<Long, ProductStock> stockMap = productStockRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(ProductStock::getProductId, ps -> ps));
        return products.map(p -> ProductListResponse.from(p, stockMap.get(p.getId())));
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
