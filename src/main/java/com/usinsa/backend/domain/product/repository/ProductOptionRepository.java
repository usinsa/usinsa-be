package com.usinsa.backend.domain.product.repository;

import com.usinsa.backend.domain.product.entity.ProductOption;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductOptionRepository extends JpaRepository<ProductOption, Long> {
}