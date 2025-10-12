package com.usinsa.backend.domain.product.service;

import com.usinsa.backend.domain.category.entity.Category;
import com.usinsa.backend.domain.category.repository.CategoryRepository;
import com.usinsa.backend.domain.product.dto.ProductDto;
import com.usinsa.backend.domain.product.dto.ProductOptionDto;
import com.usinsa.backend.domain.product.entity.Product;
import com.usinsa.backend.domain.product.entity.ProductOption;
import com.usinsa.backend.domain.product.repository.ProductOptionRepository;
import com.usinsa.backend.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductOptionRepository optionRepository;

    // 상품 등록
    public ProductDto.Response create(ProductDto.CreateReq request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("카테고리를 찾을 수 없습니다."));

        Product product = Product.builder()
                .name(request.getName())
                .brandName(request.getBrand())
                .price(request.getPrice())
                .category(category)
                .likeCount(0)
                .clickCount(0)
                .build();

        Product saved = productRepository.save(product);
        return toProductResDto(saved);
    }

    // 상품 조회
    @Transactional(readOnly = true)
    public ProductDto.Response findById(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));
        return toProductResDto(product);
    }

    // 상품 옵션 추가
    public ProductOptionDto.Response addOption(Long productId, ProductOptionDto.CreateReq request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

        ProductOption option = ProductOption.builder()
                .optionName(request.getOptionName())
                .stock(request.getStock())
                .product(product)
                .build();

        ProductOption saved = optionRepository.save(option);
        return toProductOptionResDto(saved);
    }

    // Product의 DTO 변환
    private ProductDto.Response toProductResDto(Product product) {
        return ProductDto.Response.builder()
                .id(product.getId())
                .categoryName(product.getCategory().getName())
                .name(product.getName())
                .brandName(product.getBrandName())
                .price(product.getPrice())
                .likeCount(product.getLikeCount())
                .clickCount(product.getClickCount())
                .build();
    }

    // ProductOption의 DTO 변환
    private ProductOptionDto.Response toProductOptionResDto(ProductOption option) {
        return ProductOptionDto.Response.builder()
                .id(option.getId())
                .optionName(option.getOptionName())
                .stock(option.getStock())
                .productId(option.getProduct().getId())
                .build();
    }
}