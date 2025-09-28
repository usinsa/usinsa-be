package com.usinsa.backend.domain.order.controller;

import com.usinsa.backend.domain.order.entity.OrderedProduct;
import com.usinsa.backend.domain.order.service.OrderedProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ordered-products")
@RequiredArgsConstructor
public class OrderedProductController {

    private final OrderedProductService orderedProductService;

    // 등록
    @PostMapping
    public ResponseEntity<OrderedProduct> create(
            @RequestParam Long orderId,
            @RequestParam Long productOptionId,
            @RequestParam Integer quantity
    ) {
        return ResponseEntity.ok(
                orderedProductService.create(orderId, productOptionId, quantity)
        );
    }

    // 단건 조회
    @GetMapping("/{id}")
    public ResponseEntity<OrderedProduct> get(@PathVariable Long id) {
        return ResponseEntity.ok(orderedProductService.get(id));
    }

    // 전체 조회
    @GetMapping
    public ResponseEntity<List<OrderedProduct>> getAll() {
        return ResponseEntity.ok(orderedProductService.getAll());
    }

    // 수정 (수량 변경)
    @PutMapping("/{id}")
    public ResponseEntity<OrderedProduct> updateQuantity(
            @PathVariable Long id,
            @RequestParam Integer quantity
    ) {
        return ResponseEntity.ok(orderedProductService.updateQuantity(id, quantity));
    }

    // 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        orderedProductService.delete(id);
        return ResponseEntity.noContent().build();
    }
}