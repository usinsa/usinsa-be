package com.usinsa.backend.domain.order.service;

import com.usinsa.backend.domain.member.entity.Member;
import com.usinsa.backend.domain.member.repository.MemberRepository;
import com.usinsa.backend.domain.order.dto.OrderDto;
import com.usinsa.backend.domain.order.entity.Order;
import com.usinsa.backend.domain.order.entity.OrderStatus;
import com.usinsa.backend.domain.order.repository.OrderRepository;
import org.springframework.stereotype.Service;
import lombok.*;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final MemberRepository memberRepository;


    // 주문 생성
    public OrderDto.Response create(OrderDto.CreateReq request) {
        Member member = memberRepository.findById(request.getMemberId())
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        Order order = Order.builder()
                .member(member)
                .receiverAddress(request.getReceiverAddress())
                .receiverName(request.getReceiverName())
                .receiverPhone(request.getReceiverPhone())
                .status(OrderStatus.CREATED)
                .build();

        Order savedOrder = orderRepository.save(order);
        return toResDto(savedOrder);
    }

    // 주문 수정
    public OrderDto.Response update(Long orderId, OrderDto.UpdateReq request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        order.setReceiverAddress(request.getReceiverAddress());
        order.setReceiverName(request.getReceiverName());
        order.setReceiverPhone(request.getReceiverPhone());

        return toResDto(order);
    }

    // 주문 취소
    public OrderDto.Response cancel(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        order.setStatus(OrderStatus.CANCELLED);
        return toResDto(order);
    }

    //DTO 변환
    private OrderDto.Response toResDto(Order order) {
        return OrderDto.Response.builder()
                .id(order.getId())
              //  .memberId(order.getMember().getId())
                .receiverAddress(order.getReceiverAddress())
                .receiverName(order.getReceiverName())
                .receiverPhone(order.getReceiverPhone())
                .status(order.getStatus())
                .build();
    }
}