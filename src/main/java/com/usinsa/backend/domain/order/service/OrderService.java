package com.usinsa.backend.domain.order.service;

import com.usinsa.backend.domain.delivery.dto.DeliveryDto;
import com.usinsa.backend.domain.delivery.entity.Delivery;
import com.usinsa.backend.domain.member.entity.Member;
import com.usinsa.backend.domain.member.repository.MemberRepository;
import com.usinsa.backend.domain.order.dto.OrderDto;
import com.usinsa.backend.domain.order.entity.Order;
import com.usinsa.backend.domain.order.entity.OrderStatus;
import com.usinsa.backend.domain.order.repository.OrderRepository;
import com.usinsa.backend.global.exception.CustomException;
import com.usinsa.backend.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import lombok.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final MemberRepository memberRepository;


    // 주문 생성
    public OrderDto.Response create(OrderDto.CreateReq request) {
        Member member = memberRepository.findById(request.getMemberId())
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        Order order = toEntity(request, member);
        Order savedOrder = orderRepository.save(order);

        return toResDto(savedOrder);
    }

    // 주문 단건 조회
    public OrderDto.Response findById(Long orderId) {
        Order order = orderRepository.findWithMemberAndDeliveryById(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));
        return toResDto(order);
    }

    // 주문 전체 조회
    public List<OrderDto.Response> findAll() {
        return orderRepository.findAll().stream()
                .map(this::toResDto)
                .collect(Collectors.toList());
    }

    // 주문 수정
    public OrderDto.Response update(Long orderId, OrderDto.UpdateReq request) {
        Order order = orderRepository.findWithMemberAndDeliveryById(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        order.setReceiverAddress(request.getReceiverAddress());
        order.setReceiverName(request.getReceiverName());
        order.setReceiverPhone(request.getReceiverPhone());

        return toResDto(order);
    }

    // 주문 취소
    public OrderDto.Response cancel(Long orderId) {
        Order order = orderRepository.findWithMemberAndDeliveryById(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new CustomException(ErrorCode.ORDER_ALREADY_CANCELLED);
        }

        if (order.getStatus() != OrderStatus.CREATED && order.getStatus() != OrderStatus.PAYMENT_READY) {
            throw new CustomException(ErrorCode.ORDER_CANNOT_CANCEL);
        }

        order.setStatus(OrderStatus.CANCELLED);
        return toResDto(order);
    }

    // 결제 준비 상태로 변경
    public void updateToPaymentReady(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() != OrderStatus.CREATED) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        order.setStatus(OrderStatus.PAYMENT_READY);
    }

    // 결제 완료 상태로 변경
    public void updateToPaymentCompleted(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() != OrderStatus.PAYMENT_READY) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        order.setStatus(OrderStatus.PAYMENT_COMPLETED);
    }

    // 주문 취소 (결제 취소용)
    public void updateToCancelled(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        order.setStatus(OrderStatus.CANCELLED);
    }

    // DTO -> 객체 변환
    private Order toEntity(OrderDto.CreateReq request, Member member) {
        return Order.builder()
                .member(member)
                .receiverAddress(request.getReceiverAddress())
                .receiverName(request.getReceiverName())
                .receiverPhone(request.getReceiverPhone())
                .status(OrderStatus.CREATED)
                .build();
    }

    //객체 -> DTO 변환
    private OrderDto.Response toResDto(Order order) {
        return OrderDto.Response.builder()
                .id(order.getId())
                .memberId(order.getMember().getId())
                .receiverAddress(order.getReceiverAddress())
                .receiverName(order.getReceiverName())
                .receiverPhone(order.getReceiverPhone())
                .status(order.getStatus())
                .build();
    }
}