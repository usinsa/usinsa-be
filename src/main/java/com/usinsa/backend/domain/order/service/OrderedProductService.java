package com.usinsa.backend.domain.order.service;

import com.usinsa.backend.domain.order.dto.OrderedProductReqDto;
import com.usinsa.backend.domain.order.dto.OrderedProductResDto;
import com.usinsa.backend.domain.order.entity.Order;
import com.usinsa.backend.domain.order.entity.OrderedProduct;
import com.usinsa.backend.domain.order.repository.OrderRepository;
import com.usinsa.backend.domain.order.repository.OrderedProductRepository;
import com.usinsa.backend.domain.product.entity.ProductOption;
import com.usinsa.backend.domain.product.repository.ProductOptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderedProductService {

    private final OrderedProductRepository orderedProductRepository;
    private final OrderRepository orderRepository;
    private final ProductOptionRepository productOptionRepository;

    // 등록
    public OrderedProductResDto create(OrderedProductReqDto reqDto) {
        Order order = orderRepository.findById(reqDto.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        ProductOption option = productOptionRepository.findById(reqDto.getProductOptionId())
                .orElseThrow(() -> new IllegalArgumentException("Product option not found"));

        OrderedProduct orderedProduct = OrderedProduct.builder()
                .order(order)
                .productOption(option)
                .quantity(reqDto.getQuantity())
                .build();

        return toResDto(orderedProductRepository.save(orderedProduct));
    }

    // 단건 조회
    @Transactional(readOnly = true)
    public OrderedProductResDto get(Long id) {
        return orderedProductRepository.findById(id)
                .map(this::toResDto)
                .orElseThrow(() -> new IllegalArgumentException("Ordered product not found"));
    }

    // 전체 조회
    @Transactional(readOnly = true)
    public List<OrderedProductResDto> getAll() {
        return orderedProductRepository.findAll()
                .stream()
                .map(this::toResDto)
                .collect(Collectors.toList());
    }
    // 수정 (수량 변경)
    public OrderedProductResDto updateQuantity(Long id, Integer newQuantity) {
        OrderedProduct orderedProduct = orderedProductRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ordered product not found"));

        orderedProduct.setQuantity(newQuantity);

        return toResDto(orderedProduct);
    }

    // 삭제
    public void delete(Long id) {
        OrderedProduct orderedProduct = orderedProductRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ordered product not found"));
        orderedProductRepository.delete(orderedProduct);
    }

    /* DTO 내부에서 변환 시키지 않는 이유
        기존: DTO 내부에 변환 코드 작성 -> 코드 단순화 but 의존도 상승
        신규: DTO가 엔티티를 모르는 상태를 유지해서 결합도 낮춤
     */
    private OrderedProductResDto toResDto(OrderedProduct orderedProduct) {
        return OrderedProductResDto.builder()
                .id(orderedProduct.getId())
                .orderId(orderedProduct.getOrder().getId())
                .productOptionId(orderedProduct.getProductOption().getId())
                .quantity(orderedProduct.getQuantity())
                .build();
    }

}