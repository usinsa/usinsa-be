package com.usinsa.backend.domain.deliveryAddress.controller;

import com.usinsa.backend.domain.deliveryAddress.dto.DeliveryAddressDto;
import com.usinsa.backend.domain.deliveryAddress.service.DeliveryAddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/delivery-address")
@RequiredArgsConstructor
public class DeliveryAddressController {

    private final DeliveryAddressService deliveryAddressService;

    @PostMapping
    public ResponseEntity<DeliveryAddressDto.Response> createDeliveryAddress(@RequestBody @Valid DeliveryAddressDto.CreateReq request) {
        return ResponseEntity.ok(deliveryAddressService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DeliveryAddressDto.Response> getDeliveryAddress(@PathVariable Long id) {
        return ResponseEntity.ok(deliveryAddressService.findById(id));
    }

    @GetMapping
    public ResponseEntity<List<DeliveryAddressDto.Response>> getAllDeliveryAddress() {
        return ResponseEntity.ok(deliveryAddressService.findAll());
    }

    @PutMapping("/{id}")
    public ResponseEntity<DeliveryAddressDto.Response> updateDeliveryAddress(@PathVariable Long id,
                                                         @RequestBody @Valid DeliveryAddressDto.UpdateReq request) {
        return ResponseEntity.ok(deliveryAddressService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDeliveryAddress(@PathVariable Long id) {
        deliveryAddressService.delete(id);
        return ResponseEntity.noContent().build();
    }
}