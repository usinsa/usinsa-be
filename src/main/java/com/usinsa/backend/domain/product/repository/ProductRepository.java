package com.usinsa.backend.domain.product.repository;

import com.usinsa.backend.domain.product.entity.Product;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @EntityGraph(attributePaths = {"category", "options"})
    Optional<Product> findWithCategoryAndOptionsById(Long id);

    @Override
    @EntityGraph(attributePaths = {"category", "options"})
    List<Product> findAll();

    @Query("SELECT DISTINCT p FROM Product p " +
           "JOIN FETCH p.category c " +
           "LEFT JOIN FETCH p.options " +
           "WHERE c.id = :categoryId OR c.parent.id = :categoryId")
    List<Product> findByCategoryId(@Param("categoryId") Long categoryId);
}
