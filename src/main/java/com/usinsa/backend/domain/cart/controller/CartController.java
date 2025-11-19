package com.usinsa.backend.domain.cart.controller;

import com.usinsa.backend.domain.cart.dto.CartDto;
import com.usinsa.backend.domain.cart.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/carts")
public class CartController {

    private final CartService cartService;

    @PostMapping
    public ResponseEntity<CartDto.Response> createCart(@RequestBody CartDto.CreateReq request) {
        return ResponseEntity.ok(cartService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CartDto.Response> getCart(@PathVariable Long id) {
        return ResponseEntity.ok(cartService.findById(id));
    }

    @GetMapping
    public ResponseEntity<List<CartDto.Response>> getAllCarts() {
        return ResponseEntity.ok(cartService.findAll());
    }

    @PutMapping("/{id}")
    public ResponseEntity<CartDto.Response> updateCart(
            @PathVariable Long id,
            @RequestBody CartDto.UpdateReq request
    ) {
        return ResponseEntity.ok(cartService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCart(@PathVariable Long id) {
        cartService.delete(id);
        return ResponseEntity.noContent().build();
    }
}