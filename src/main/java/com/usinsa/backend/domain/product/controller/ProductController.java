package com.usinsa.backend.domain.product.controller;

import com.usinsa.backend.domain.product.dto.ProductDto;
import com.usinsa.backend.domain.product.dto.ProductOptionDto;
import com.usinsa.backend.domain.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    // 상품 등록
    @PostMapping
    public ResponseEntity<ProductDto.Response> createProduct(@RequestBody ProductDto.CreateReq request) {
        return ResponseEntity.ok(productService.createProduct(request));
    }

    // 상품 조회
    @GetMapping("/{id}")
    public ResponseEntity<ProductDto.Response> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    // 상품 옵션 추가
    @PostMapping("/{productId}/options")
    public ResponseEntity<ProductOptionDto.Response> addOption(
            @PathVariable Long productId,
            @RequestBody ProductOptionDto.CreateReq request) {
        return ResponseEntity.ok(productService.addOption(productId, request));
    }
}