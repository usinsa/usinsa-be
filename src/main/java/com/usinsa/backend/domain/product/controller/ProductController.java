package com.usinsa.backend.domain.product.controller;

import com.usinsa.backend.domain.product.entity.Product;
import com.usinsa.backend.domain.product.entity.ProductOption;
import com.usinsa.backend.domain.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    /* ********************DTO 추가시 변경***************** */

    @PostMapping
    public ResponseEntity<Product> createProduct(
            @RequestParam Long categoryId,
            @RequestParam String name,
            @RequestParam String brand,
            @RequestParam Long price) {

        Product product = productService.createProduct(categoryId, name, brand, price);
        return ResponseEntity.ok(product);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @PostMapping("/{productId}/options")
    public ResponseEntity<ProductOption> addOption(@PathVariable Long productId,
                                                   @RequestBody ProductOption option) {
        return ResponseEntity.ok(productService.addOption(productId, option));
    }
}