package com.usinsa.backend.domain.order.controller;

import com.usinsa.backend.domain.order.dto.OrderedProductDto;
import com.usinsa.backend.domain.order.service.OrderedProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ordered-products")
@RequiredArgsConstructor
public class OrderedProductController {

    private final OrderedProductService orderedProductService;

    // 등록
    @PostMapping
    public ResponseEntity<OrderedProductDto.Response> createOrderedProduct(@RequestBody OrderedProductDto.Request reqDto) {
        return ResponseEntity.ok(orderedProductService.create(reqDto));
    }

    // 단건 조회
    @GetMapping("/{id}")
    public ResponseEntity<OrderedProductDto.Response> getOrderedProduct(@PathVariable Long id) {
        return ResponseEntity.ok(orderedProductService.findById(id));
    }

    // 전체 조회
    @GetMapping
    public ResponseEntity<List<OrderedProductDto.Response>> getAllOrderedProduct() {
        return ResponseEntity.ok(orderedProductService.findAll());
    }

    // 수정 (수량 변경)
    @PutMapping("/{id}/quantity")
    public ResponseEntity<OrderedProductDto.Response> updateQuantity(@PathVariable Long id,
                                                                     @RequestParam Integer quantity) {
        return ResponseEntity.ok(orderedProductService.updateQuantity(id, quantity));
    }

    // 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrderedProduct(@PathVariable Long id) {
        orderedProductService.delete(id);
        return ResponseEntity.noContent().build();
    }
}