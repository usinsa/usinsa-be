package com.usinsa.backend.domain.product.controller;

import com.usinsa.backend.domain.product.dto.ProductDto;
import com.usinsa.backend.domain.product.dto.ProductOptionDto;
import com.usinsa.backend.domain.product.service.ProductService;
import com.usinsa.backend.domain.search.elastic.service.ProductReindexService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;
    private final ProductReindexService reindexService;

    // 상품 등록
    @PostMapping
    public ResponseEntity<ProductDto.Response> createProduct(@RequestBody ProductDto.CreateReq request) {
        return ResponseEntity.ok(productService.create(request));
    }

    // 상품 수정
    @PutMapping("/{id}")
    public ResponseEntity<ProductDto.Response> updateProduct(
            @PathVariable Long id,
            @RequestBody ProductDto.CreateReq request
    ) {
        return ResponseEntity.ok(productService.update(id, request));
    }

    // 상품 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // 상품 단건 조회
    @GetMapping("/{id}")
    public ResponseEntity<ProductDto.Response> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(productService.findById(id));
    }

    // 상품 전체 조회
    @GetMapping
    public ResponseEntity<List<ProductDto.Response>> getAllProducts() {
        return ResponseEntity.ok(productService.findAll());
    }

    // 카테고리별 상품 조회
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<ProductDto.Response>> getProductsByCategory(@PathVariable Long categoryId) {
        return ResponseEntity.ok(productService.findByCategoryId(categoryId));
    }

    // 상품 옵션 추가
    @PostMapping("/{productId}/options")
    public ResponseEntity<ProductOptionDto.Response> addOption(
            @PathVariable Long productId,
            @RequestBody ProductOptionDto.CreateReq request) {
        return ResponseEntity.ok(productService.addOption(productId, request));
    }

    @PostMapping("/reindex")
    public ResponseEntity<String> rebuildIndex() {

        int count = reindexService.reindexAll();

        return ResponseEntity.ok("Reindexed " + count + " products into Elasticsearch.");
    }
}