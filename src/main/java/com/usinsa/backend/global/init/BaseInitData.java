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
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        init();
    }

    private void init() {
        // 회원 생성
        Member member = Member.builder()
                .usinaId("1234@test.com")
                .email("1234@test.com")
                .password(passwordEncoder.encode("1234"))
                .name("홍길동")
                .nickname("길동이")
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

        // ===== 추가 상품 =====
        Product paddingJacket = Product.builder()
                .name("숏 패딩 자켓")
                .brandName("유신사 스탠다드")
                .price(129000L)
                .likeCount(0)
                .clickCount(0)
                .category(jacket)
                .build();

        Product longCoat = Product.builder()
                .name("롱 울 코트")
                .brandName("유신사 스탠다드")
                .price(199000L)
                .likeCount(0)
                .clickCount(0)
                .category(coat)
                .build();

        Product oxfordShirt = Product.builder()
                .name("옥스포드 셔츠")
                .brandName("유신사 스탠다드")
                .price(69000L)
                .likeCount(0)
                .clickCount(0)
                .category(shirt)
                .build();

        Product blackTee = Product.builder()
                .name("블랙 티셔츠")
                .brandName("유신사 스탠다드")
                .price(19900L)
                .likeCount(0)
                .clickCount(0)
                .category(tShirt)
                .build();

        Product skinnyJeans = Product.builder()
                .name("스키니 진")
                .brandName("유신사 스탠다드")
                .price(109000L)
                .likeCount(0)
                .clickCount(0)
                .category(jeans)
                .build();

        Product graySlacks = Product.builder()
                .name("그레이 슬랙스")
                .brandName("유신사 스탠다드")
                .price(79000L)
                .likeCount(0)
                .clickCount(0)
                .category(slacks)
                .build();

        productRepository.saveAll(List.of(
                paddingJacket, longCoat, oxfordShirt,
                blackTee, skinnyJeans, graySlacks
        ));

        // ===== 추가 상품 옵션 =====
        List<ProductOption> extraOptions = List.of(
                // 숏 패딩 자켓
                ProductOption.builder().product(paddingJacket).optionName("S").stock(20).build(),
                ProductOption.builder().product(paddingJacket).optionName("M").stock(30).build(),
                ProductOption.builder().product(paddingJacket).optionName("L").stock(25).build(),

                // 롱 울 코트
                ProductOption.builder().product(longCoat).optionName("M").stock(15).build(),
                ProductOption.builder().product(longCoat).optionName("L").stock(10).build(),

                // 옥스포드 셔츠
                ProductOption.builder().product(oxfordShirt).optionName("M").stock(40).build(),
                ProductOption.builder().product(oxfordShirt).optionName("L").stock(35).build(),
                ProductOption.builder().product(oxfordShirt).optionName("XL").stock(20).build(),

                // 블랙 티셔츠
                ProductOption.builder().product(blackTee).optionName("S").stock(80).build(),
                ProductOption.builder().product(blackTee).optionName("M").stock(100).build(),
                ProductOption.builder().product(blackTee).optionName("L").stock(70).build(),

                // 스키니 진
                ProductOption.builder().product(skinnyJeans).optionName("30").stock(25).build(),
                ProductOption.builder().product(skinnyJeans).optionName("32").stock(30).build(),
                ProductOption.builder().product(skinnyJeans).optionName("34").stock(20).build(),

                // 그레이 슬랙스
                ProductOption.builder().product(graySlacks).optionName("30").stock(20).build(),
                ProductOption.builder().product(graySlacks).optionName("32").stock(25).build(),
                ProductOption.builder().product(graySlacks).optionName("34").stock(15).build(),
                ProductOption.builder().product(leatherJacket).optionName("S").stock(15).build(),
                ProductOption.builder().product(leatherJacket).optionName("XL").stock(10).build(),

                // 트렌치 코트 (옵션 신규 추가)
                ProductOption.builder().product(trenchCoat).optionName("M").stock(20).build(),
                ProductOption.builder().product(trenchCoat).optionName("L").stock(15).build(),

                // 베이직 셔츠 (옵션 신규 추가)
                ProductOption.builder().product(basicShirt).optionName("S").stock(40).build(),
                ProductOption.builder().product(basicShirt).optionName("M").stock(50).build(),
                ProductOption.builder().product(basicShirt).optionName("L").stock(35).build(),

                // 화이트 티셔츠 (기존: M → 추가)
                ProductOption.builder().product(whiteTee).optionName("S").stock(80).build(),
                ProductOption.builder().product(whiteTee).optionName("L").stock(70).build(),
                ProductOption.builder().product(whiteTee).optionName("XL").stock(50).build(),

                // 블루 진 (기존: 32 → 추가)
                ProductOption.builder().product(blueJeans).optionName("30").stock(20).build(),
                ProductOption.builder().product(blueJeans).optionName("34").stock(15).build(),

                // 블랙 슬랙스 (옵션 신규 추가)
                ProductOption.builder().product(blackSlacks).optionName("30").stock(25).build(),
                ProductOption.builder().product(blackSlacks).optionName("32").stock(30).build(),
                ProductOption.builder().product(blackSlacks).optionName("34").stock(20).build()
        );

        optionRepository.saveAll(extraOptions);
    }
}