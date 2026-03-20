package com.drf.product.repository;

import com.drf.product.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByParentId(Long parentId);

    boolean existsByParentIdIsNullAndName(String name);

    boolean existsByParentIdAndName(Long parentId, String name);
}

