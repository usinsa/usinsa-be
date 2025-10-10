package com.usinsa.backend.domain.delivery.service;

import com.usinsa.backend.domain.delivery.dto.DeliveryDto;
import com.usinsa.backend.domain.delivery.entity.Delivery;
import com.usinsa.backend.domain.delivery.entity.DeliveryStatus;
import com.usinsa.backend.domain.delivery.repository.DeliveryRepository;
import com.usinsa.backend.domain.order.entity.Order;
import com.usinsa.backend.domain.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final OrderRepository orderRepository;

    @Transactional
    public DeliveryDto.Response create(DeliveryDto.CreateReq request) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다."));

        Delivery delivery = Delivery.builder()
                .order(order)
                .trackingNumber(request.getTrackingNumber())
                .deliveryStatus(request.getDeliveryStatus() != null ? request.getDeliveryStatus() : DeliveryStatus.READY)
                .build();

        Delivery saved = deliveryRepository.save(delivery);
        return DeliveryDto.Response.fromEntity(saved);
    }

    public DeliveryDto.Response getById(Long deliveryId) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new IllegalArgumentException("배송 정보를 찾을 수 없습니다."));
        return DeliveryDto.Response.fromEntity(delivery);
    }

    public List<DeliveryDto.Response> getAll() {
        return deliveryRepository.findAll().stream()
                .map(DeliveryDto.Response::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public DeliveryDto.Response update(Long deliveryId, DeliveryDto.CreateReq request) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new IllegalArgumentException("배송 정보를 찾을 수 없습니다."));

        if (request.getTrackingNumber() != null)
            delivery.updateTrackingNumber(request.getTrackingNumber());

        if (request.getDeliveryStatus() != null)
            delivery.updateDeliveryStatus(request.getDeliveryStatus());

        return DeliveryDto.Response.fromEntity(delivery);
    }

    @Transactional
    public void delete(Long deliveryId) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new IllegalArgumentException("배송 정보를 찾을 수 없습니다."));
        deliveryRepository.delete(delivery);
    }
}