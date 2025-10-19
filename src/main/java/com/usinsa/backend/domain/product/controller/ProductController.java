package com.usinsa.backend.domain.product.controller;

import com.usinsa.backend.domain.order.dto.OrderDto;
import com.usinsa.backend.domain.product.dto.ProductDto;
import com.usinsa.backend.domain.product.dto.ProductOptionDto;
import com.usinsa.backend.domain.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    // 상품 등록
    @PostMapping
    public ResponseEntity<ProductDto.Response> createProduct(@RequestBody ProductDto.CreateReq request) {
        return ResponseEntity.ok(productService.create(request));
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

    // 상품 옵션 추가
    @PostMapping("/{productId}/options")
    public ResponseEntity<ProductOptionDto.Response> addOption(
            @PathVariable Long productId,
            @RequestBody ProductOptionDto.CreateReq request) {
        return ResponseEntity.ok(productService.addOption(productId, request));
    }
}