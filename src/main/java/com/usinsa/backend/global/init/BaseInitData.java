package com.usinsa.backend.global.init;

import com.usinsa.backend.domain.category.entity.Category;
import com.usinsa.backend.domain.category.repository.CategoryRepository;
import com.usinsa.backend.domain.delivery.entity.Delivery;
import com.usinsa.backend.domain.delivery.entity.DeliveryStatus;
import com.usinsa.backend.domain.delivery.repository.DeliveryRepository;
import com.usinsa.backend.domain.member.entity.Member;
import com.usinsa.backend.domain.member.repository.MemberRepository;
import com.usinsa.backend.domain.order.entity.Order;
import com.usinsa.backend.domain.order.entity.OrderStatus;
import com.usinsa.backend.domain.order.repository.OrderRepository;
import com.usinsa.backend.domain.product.entity.Product;
import com.usinsa.backend.domain.product.entity.ProductOption;
import com.usinsa.backend.domain.product.repository.ProductOptionRepository;
import com.usinsa.backend.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class BaseInitData implements CommandLineRunner {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductOptionRepository optionRepository;
    private final OrderRepository orderRepository;
    private final DeliveryRepository deliveryRepository;
    private final MemberRepository memberRepository;

    @Override
    @Transactional
    public void run(String... args) {
        init();
    }

    private void init() {
        // 회원 생성
        Member member = Member.builder()
                .usinaId("user01")
                .password("1234")
                .name("홍길동")
                .nickname("길동이")
                .email("hong@test.com")
                .phone("010-1111-2222")
                .isAdmin(false)
                .build();
        memberRepository.save(member);

        // 카테고리 저장 (상위 → 하위 순서)
        Category outer = categoryRepository.save(Category.builder().name("아우터").build());
        Category top = categoryRepository.save(Category.builder().name("상의").build());
        Category bottom = categoryRepository.save(Category.builder().name("하의").build());

        Category jacket = Category.builder().name("자켓").parent(outer).build();
        Category coat = Category.builder().name("코트").parent(outer).build();
        Category shirt = Category.builder().name("셔츠").parent(top).build();
        Category tShirt = Category.builder().name("티셔츠").parent(top).build();
        Category jeans = Category.builder().name("청바지").parent(bottom).build();
        Category slacks = Category.builder().name("슬랙스").parent(bottom).build();

        categoryRepository.saveAll(List.of(jacket, coat, shirt, tShirt, jeans, slacks));

        // 상품 저장
        Product leatherJacket = Product.builder()
                .name("가죽 자켓")
                .brandName("유신사 스탠다드")
                .price(89000L)
                .likeCount(0)
                .clickCount(0)
                .category(jacket)
                .build();

        Product trenchCoat = Product.builder()
                .name("트렌치 코트")
                .brandName("유신사 스탠다드")
                .price(149000L)
                .likeCount(0)
                .clickCount(0)
                .category(coat)
                .build();

        Product basicShirt = Product.builder()
                .name("베이직 셔츠")
                .brandName("유신사 스탠다드")
                .price(59000L)
                .likeCount(0)
                .clickCount(0)
                .category(shirt)
                .build();

        Product whiteTee = Product.builder()
                .name("화이트 티셔츠")
                .brandName("유신사 스탠다드")
                .price(19900L)
                .likeCount(0)
                .clickCount(0)
                .category(tShirt)
                .build();

        Product blueJeans = Product.builder()
                .name("블루 진")
                .brandName("유신사 스탠다드")
                .price(99000L)
                .likeCount(0)
                .clickCount(0)
                .category(jeans)
                .build();

        Product blackSlacks = Product.builder()
                .name("블랙 슬랙스")
                .brandName("유신사 스탠다드")
                .price(69000L)
                .likeCount(0)
                .clickCount(0)
                .category(slacks)
                .build();

        productRepository.saveAll(List.of(leatherJacket, trenchCoat, basicShirt, whiteTee, blueJeans, blackSlacks));

        // 상품 옵션 저장
        ProductOption jacketM = ProductOption.builder()
                .product(leatherJacket)
                .optionName("M")
                .stock(30)
                .build();

        ProductOption jacketL = ProductOption.builder()
                .product(leatherJacket)
                .optionName("L")
                .stock(25)
                .build();

        ProductOption tshirtM = ProductOption.builder()
                .product(whiteTee)
                .optionName("M")
                .stock(100)
                .build();

        ProductOption jeans32 = ProductOption.builder()
                .product(blueJeans)
                .optionName("32")
                .stock(50)
                .build();

        optionRepository.saveAll(List.of(jacketM, jacketL, tshirtM, jeans32));

        // 주문 & 배송
        Order order1 = Order.builder()
                .member(member)
                .receiverName("홍길동")
                .receiverPhone("010-2222-3333")
                .receiverAddress("서울특별시 종로구 청와대로 1")
                .status(OrderStatus.CREATED)
                .build();
        orderRepository.save(order1);

        Delivery delivery1 = Delivery.builder()
                .order(order1)
                .trackingNumber("001234")
                .deliveryStatus(DeliveryStatus.READY)
                .build();
        deliveryRepository.save(delivery1);
    }
}