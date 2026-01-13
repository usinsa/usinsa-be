package com.usinsa.backend.domain.delivery.service;

import com.usinsa.backend.domain.delivery.dto.DeliveryDto;
import com.usinsa.backend.domain.delivery.entity.Delivery;
import com.usinsa.backend.domain.delivery.entity.DeliveryStatus;
import com.usinsa.backend.domain.delivery.repository.DeliveryRepository;
import com.usinsa.backend.domain.order.entity.Order;
import com.usinsa.backend.domain.order.repository.OrderRepository;
import com.usinsa.backend.global.exception.CustomException;
import com.usinsa.backend.global.exception.ErrorCode;
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
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        Delivery delivery = toEntity(request, order);
        Delivery saved = deliveryRepository.save(delivery);
        return toResDto(saved);
    }

    @Transactional(readOnly = true)
    public DeliveryDto.Response findById(Long deliveryId) {
        Delivery delivery = deliveryRepository.findWithOrderById(deliveryId)
                .orElseThrow(() -> new CustomException(ErrorCode.DELIVERY_NOT_FOUND));
        return toResDto(delivery);
    }

    @Transactional(readOnly = true)
    public List<DeliveryDto.Response> findAll() {
        return deliveryRepository.findAll().stream()
                .map(this::toResDto)
                .collect(Collectors.toList());
    }

    public DeliveryDto.Response update(Long deliveryId, DeliveryDto.CreateReq request) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new CustomException(ErrorCode.DELIVERY_NOT_FOUND));

        if (request.getTrackingNumber() != null)
            delivery.updateTrackingNumber(request.getTrackingNumber());

        if (request.getDeliveryStatus() != null)
            delivery.updateDeliveryStatus(request.getDeliveryStatus());

        return toResDto(delivery);
    }

    public void delete(Long deliveryId) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new CustomException(ErrorCode.DELIVERY_NOT_FOUND));
        deliveryRepository.delete(delivery);
    }

    private Delivery toEntity(DeliveryDto.CreateReq request, Order order) {
        return Delivery.builder()
                .order(order)
                .trackingNumber(request.getTrackingNumber())
                .deliveryStatus(request.getDeliveryStatus() != null ? request.getDeliveryStatus() : DeliveryStatus.READY)
                .build();
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