package com.drf.product.repository;

import com.drf.product.entity.ProductStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductStockRepository extends JpaRepository<ProductStock, Long> {

    @Modifying
    @Query("UPDATE ProductStock ps SET ps.stock = ps.stock - :quantity WHERE ps.productId = :productId AND ps.stock >= :quantity")
    int decrementStock(@Param("productId") long productId, @Param("quantity") int quantity);

    @Modifying
    @Query("UPDATE ProductStock ps SET ps.stock = ps.stock + :quantity WHERE ps.productId = :productId")
    int incrementStock(@Param("productId") long productId, @Param("quantity") int quantity);
}
