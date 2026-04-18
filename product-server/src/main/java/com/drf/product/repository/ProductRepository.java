package com.drf.product.repository;

import com.drf.product.entity.Product;
import com.drf.product.entity.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @EntityGraph(attributePaths = {"category"})
    Page<Product> findByNameContainingAndStatusNot(String name, ProductStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"category"})
    Page<Product> findByCategoryIdInAndStatusNot(Collection<Long> categoryIds, ProductStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"category", "stock"})
    List<Product> findByIdIn(Collection<Long> ids);

    boolean existsByCategoryId(Long categoryId);
}
