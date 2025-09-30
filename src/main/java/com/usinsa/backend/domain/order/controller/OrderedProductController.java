package com.usinsa.backend.domain.order.controller;

import com.usinsa.backend.domain.order.dto.OrderedProductReqDto;
import com.usinsa.backend.domain.order.dto.OrderedProductResDto;
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
    public ResponseEntity<OrderedProductResDto> create(@RequestBody OrderedProductReqDto reqDto) {
        return ResponseEntity.ok(orderedProductService.create(reqDto));
    }

    // 단건 조회
    @GetMapping("/{id}")
    public ResponseEntity<OrderedProductResDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(orderedProductService.get(id));
    }

    // 전체 조회
    @GetMapping
    public ResponseEntity<List<OrderedProductResDto>> getAll() {
        return ResponseEntity.ok(orderedProductService.getAll());
    }

    // 수정 (수량 변경)
    @PutMapping("/{id}")
    public ResponseEntity<OrderedProductResDto> updateQuantity(
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