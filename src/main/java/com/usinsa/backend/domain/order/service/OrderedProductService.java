package com.usinsa.backend.domain.order.service;

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

@Service
@RequiredArgsConstructor
@Transactional
public class OrderedProductService {

    private final OrderedProductRepository orderedProductRepository;
    private final OrderRepository orderRepository;
    private final ProductOptionRepository productOptionRepository;

    // 등록
    public OrderedProduct create(Long orderId, Long productOptionId, Integer quantity) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        ProductOption option = productOptionRepository.findById(productOptionId)
                .orElseThrow(() -> new IllegalArgumentException("Product option not found"));

        OrderedProduct orderedProduct = OrderedProduct.builder()
                .order(order)
                .productOption(option)
                .quantity(quantity)
                .build();

        return orderedProductRepository.save(orderedProduct);
    }

    // 단건 조회
    @Transactional(readOnly = true)
    public OrderedProduct get(Long id) {
        return orderedProductRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ordered product not found"));
    }

    // 전체 조회
    @Transactional(readOnly = true)
    public List<OrderedProduct> getAll() {
        return orderedProductRepository.findAll();
    }

    // 수정 (수량 변경)
    public OrderedProduct updateQuantity(Long id, Integer newQuantity) {
        OrderedProduct orderedProduct = get(id);
        orderedProduct.setQuantity(newQuantity);
        return orderedProduct;
    }

    // 삭제
    public void delete(Long id) {
        OrderedProduct orderedProduct = get(id);
        orderedProductRepository.delete(orderedProduct);
    }
}