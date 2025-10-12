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
@Transactional
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final OrderRepository orderRepository;

    public DeliveryDto.Response create(DeliveryDto.CreateReq request) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다."));

        Delivery delivery = Delivery.builder()
                .order(order)
                .trackingNumber(request.getTrackingNumber())
                .deliveryStatus(request.getDeliveryStatus() != null ? request.getDeliveryStatus() : DeliveryStatus.READY)
                .build();

        Delivery saved = deliveryRepository.save(delivery);
        return toResDto(saved);
    }

    public DeliveryDto.Response findById(Long deliveryId) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new IllegalArgumentException("배송 정보를 찾을 수 없습니다."));
        return toResDto(delivery);
    }

    public List<DeliveryDto.Response> findAll() {
        return deliveryRepository.findAll().stream()
                .map(this::toResDto)
                .collect(Collectors.toList());
    }

    public DeliveryDto.Response update(Long deliveryId, DeliveryDto.CreateReq request) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new IllegalArgumentException("배송 정보를 찾을 수 없습니다."));

        if (request.getTrackingNumber() != null)
            delivery.updateTrackingNumber(request.getTrackingNumber());

        if (request.getDeliveryStatus() != null)
            delivery.updateDeliveryStatus(request.getDeliveryStatus());

        return toResDto(delivery);
    }

    public void delete(Long deliveryId) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new IllegalArgumentException("배송 정보를 찾을 수 없습니다."));
        deliveryRepository.delete(delivery);
    }

    private DeliveryDto.Response toResDto(Delivery Delivery) {
        return DeliveryDto.Response.builder()
                .id(Delivery.getId())
                .orderId(Delivery.getOrder().getId())
                .trackingNumber(Delivery.getTrackingNumber())
                .deliveryStatus(Delivery.getDeliveryStatus())
                .build();
    }
}