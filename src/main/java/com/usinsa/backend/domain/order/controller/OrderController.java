package com.usinsa.backend.domain.order.controller;

import com.usinsa.backend.domain.order.dto.OrderDto;
import com.usinsa.backend.domain.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderDto.Response> createOrder(@RequestBody OrderDto.CreateReq request) {
        return ResponseEntity.ok(orderService.create(request));
    }

    @PutMapping("/{orderId}")
    public ResponseEntity<OrderDto.Response> updateOrder(@PathVariable Long orderId,
                                                         @RequestBody OrderDto.UpdateReq request) {
        return ResponseEntity.ok(orderService.update(orderId, request));
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<OrderDto.Response> cancelOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.cancel(orderId));
    }
}