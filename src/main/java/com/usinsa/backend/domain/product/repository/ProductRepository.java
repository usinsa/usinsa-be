package com.usinsa.backend.domain.product.repository;

import com.usinsa.backend.domain.product.entity.Product;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.awt.geom.AffineTransform;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @EntityGraph(attributePaths = {"category", "options"})
    Optional<Product> findWithCategoryAndOptionsById(Long id);

    @Override
    @EntityGraph(attributePaths = {"category", "options"})
    List<Product> findAll();

}
