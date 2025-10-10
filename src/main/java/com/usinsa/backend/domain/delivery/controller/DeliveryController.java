package com.usinsa.backend.domain.delivery.controller;

import com.usinsa.backend.domain.delivery.dto.DeliveryDto;
import com.usinsa.backend.domain.delivery.service.DeliveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/deliveries")
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryService deliveryService;

    @PostMapping
    public ResponseEntity<DeliveryDto.Response> createDelivery(@RequestBody DeliveryDto.CreateReq request) {
        return ResponseEntity.ok(deliveryService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DeliveryDto.Response> getDelivery(@PathVariable Long id) {
        return ResponseEntity.ok(deliveryService.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<DeliveryDto.Response>> getAllDeliveries() {
        return ResponseEntity.ok(deliveryService.getAll());
    }

    @PutMapping("/{id}")
    public ResponseEntity<DeliveryDto.Response> updateDelivery(
            @PathVariable Long id,
            @RequestBody DeliveryDto.CreateReq request) {
        return ResponseEntity.ok(deliveryService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDelivery(@PathVariable Long id) {
        deliveryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}