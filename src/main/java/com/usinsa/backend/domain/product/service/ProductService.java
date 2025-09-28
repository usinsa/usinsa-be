package com.usinsa.backend.domain.product.service;

import com.usinsa.backend.domain.category.entity.Category;
import com.usinsa.backend.domain.category.repository.CategoryRepository;
import com.usinsa.backend.domain.product.entity.Product;
import com.usinsa.backend.domain.product.entity.ProductOption;
import com.usinsa.backend.domain.product.repository.ProductOptionRepository;
import com.usinsa.backend.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductOptionRepository optionRepository;

    /* ********************DTO 추가시 변경***************** */

    // 상품 등록
    @Transactional
    public Product createProduct(Long categoryId, String name, String brand, Long price) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("카테고리를 찾을 수 없습니다."));

        Product product = Product.builder()
                .name(name)
                .brandName(brand)
                .price(price)
                .category(category)
                .likeCount(0)
                .clickCount(0)
                .build();

        return productRepository.save(product);
    }

    // 상품 조회
    @Transactional(readOnly = true)
    public Product getProductById(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));
    }

    // 상품 옵션 추가
    @Transactional
    public ProductOption addOption(Long productId, ProductOption option) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

        product.addOption(option);
        return optionRepository.save(option);
    }
}