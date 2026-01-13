package com.usinsa.backend.domain.product.service;

import com.usinsa.backend.domain.category.entity.Category;
import com.usinsa.backend.domain.category.repository.CategoryRepository;
import com.usinsa.backend.domain.product.cache.ProductLikeCacheService;
import com.usinsa.backend.domain.product.dto.ProductDto;
import com.usinsa.backend.domain.product.dto.ProductOptionDto;
import com.usinsa.backend.domain.product.entity.Product;
import com.usinsa.backend.domain.product.entity.ProductOption;
import com.usinsa.backend.domain.product.repository.ProductOptionRepository;
import com.usinsa.backend.domain.product.repository.ProductRepository;
import com.usinsa.backend.domain.search.elastic.event.ProductDeletedEvent;
import com.usinsa.backend.domain.search.elastic.event.ProductSavedEvent;
import com.usinsa.backend.domain.search.elastic.event.ProductUpdatedEvent;
import com.usinsa.backend.global.exception.CustomException;
import com.usinsa.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductOptionRepository optionRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final com.usinsa.backend.domain.product.repository.ProductLikeRepository productLikeRepository;
    private final ProductLikeCacheService likeCacheService;

    // 상품 등록
    public ProductDto.Response create(ProductDto.CreateReq request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new CustomException(ErrorCode.CATEGORY_NOT_FOUND));

        Product product = toEntity(request, category);
        Product saved = productRepository.save(product);

        // ElasticSearch 저장 이벤트
        eventPublisher.publishEvent(new ProductSavedEvent(saved));

        return toProductResDto(saved);
    }

    // 상품 수정
    public ProductDto.Response update(Long productId, ProductDto.CreateReq request) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        // 업데이트
        product.update(
                request.getName(),
                request.getBrand(),
                request.getPrice()
        );

        eventPublisher.publishEvent(new ProductUpdatedEvent(product)); // ES 업데이트 이벤트

        return toProductResDto(product);
    }

    // 상품 삭제
    public void delete(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        productRepository.delete(product);
        eventPublisher.publishEvent(new ProductDeletedEvent(productId));
        
        // 캐시 무효화
        likeCacheService.invalidateProductCache(productId);
    }

    // 옵션 추가
    public ProductOptionDto.Response addOption(Long productId, ProductOptionDto.CreateReq request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        ProductOption option = toEntity(request, product);
        ProductOption saved = optionRepository.save(option);

        return toProductOptionResDto(saved);
    }

    // 상품 단건 조회
    @Transactional(readOnly = true)
    public ProductDto.Response findById(Long productId) {
        Product product = productRepository.findWithCategoryAndOptionsById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
        return toProductResDto(product);
    }

    // 상품 전체 조회
    @Transactional(readOnly = true)
    public List<ProductDto.Response> findAll() {
        return productRepository.findAll().stream()
                .map(this::toProductResDto)
                .collect(Collectors.toList());
    }

    // 전체 재색인 수행
    public int rebuildIndex() {
        List<Product> allProducts = productRepository.findAll();

        allProducts.forEach(product ->
                eventPublisher.publishEvent(new ProductSavedEvent(product))
        );

        return allProducts.size();
    }

    private Product toEntity(ProductDto.CreateReq request, Category category) {
        return Product.builder()
                .name(request.getName())
                .brandName(request.getBrand())
                .price(request.getPrice())
                .category(category)
                .likeCount(0)
                .clickCount(0)
                .build();
    }

    private ProductOption toEntity(ProductOptionDto.CreateReq request, Product product) {
        return ProductOption.builder()
                .optionName(request.getOptionName())
                .stock(request.getStock())
                .product(product)
                .build();
    }

    // Product의 DTO 변환 (Cache Aside Pattern)
    private ProductDto.Response toProductResDto(Product product) {
        // 1. 캐시에서 좋아요 개수 조회 시도
        Integer cachedLikeCount = likeCacheService.getLikeCount(product.getId());
        
        int likeCount;
        if (cachedLikeCount != null) {
            // 캐시 히트
            likeCount = cachedLikeCount;
            log.debug("상품 좋아요 개수 캐시 히트: productId={}, count={}", product.getId(), likeCount);
        } else {
            // 캐시 미스 - DB에서 조회 후 캐시 저장
            likeCount = productLikeRepository.countByProductId(product.getId());
            likeCacheService.setLikeCount(product.getId(), likeCount);
            log.debug("상품 좋아요 개수 캐시 미스 - DB 조회: productId={}, count={}", product.getId(), likeCount);
        }
        
        // 옵션 정보 변환
        List<ProductOptionDto.Response> options = product.getOptions().stream()
                .map(this::toProductOptionResDto)
                .collect(Collectors.toList());
        
        return ProductDto.Response.builder()
                .id(product.getId())
                .categoryName(product.getCategory().getName())
                .name(product.getName())
                .brandName(product.getBrandName())
                .price(product.getPrice())
                .likeCount(likeCount)
                .clickCount(product.getClickCount())
                .options(options)
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