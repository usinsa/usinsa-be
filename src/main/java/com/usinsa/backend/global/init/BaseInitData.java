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
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 개발 환경 초기 데이터
 *
 * 카테고리 저장 순서 (FE categoryId와 일치):
 *   1: 상의  2: 하의  3: 아우터  4: 신발
 */
@Slf4j
@Component
@org.springframework.core.annotation.Order(1)
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
        if (categoryRepository.count() > 0) {
            log.info("초기 데이터가 이미 존재합니다. 건너뜁니다.");
            return;
        }
        log.info("BaseInitData 초기 데이터 삽입 시작...");
        init();
        log.info("BaseInitData 초기 데이터 삽입 완료");
    }

    private void init() {
        // ── 회원 ──────────────────────────────────────────────────────
        Member member = memberRepository.save(Member.builder()
                .usinaId("test@test.com")
                .email("test@test.com")
                .password(passwordEncoder.encode("1234"))
                .name("홍길동")
                .nickname("길동이")
                .phone("010-1111-2222")
                .isAdmin(false)
                .build());

        // ── 상위 카테고리 (FE categoryId 1~4와 일치하도록 순서 고정) ──
        Category 상의   = categoryRepository.save(cat(null, "상의"));   // id=1
        Category 하의   = categoryRepository.save(cat(null, "하의"));   // id=2
        Category 아우터 = categoryRepository.save(cat(null, "아우터")); // id=3
        Category 신발   = categoryRepository.save(cat(null, "신발"));   // id=4

        // ── 하위 카테고리 ──────────────────────────────────────────────
        Category 티셔츠  = categoryRepository.save(cat(상의, "티셔츠"));
        Category 셔츠    = categoryRepository.save(cat(상의, "셔츠"));
        Category 청바지  = categoryRepository.save(cat(하의, "청바지"));
        Category 슬랙스  = categoryRepository.save(cat(하의, "슬랙스"));
        Category 자켓    = categoryRepository.save(cat(아우터, "자켓"));
        Category 코트    = categoryRepository.save(cat(아우터, "코트"));
        Category 스니커즈 = categoryRepository.save(cat(신발, "스니커즈"));
        Category 로퍼    = categoryRepository.save(cat(신발, "로퍼"));

        // ── 상품 ──────────────────────────────────────────────────────
        Product whiteTee = prod("화이트 티셔츠", "유신사 스탠다드", 19900L, 티셔츠);
        Product blackTee = prod("블랙 티셔츠", "유신사 스탠다드", 19900L, 티셔츠);
        Product basicShirt = prod("베이직 셔츠", "유신사 스탠다드", 59000L, 셔츠);
        Product oxfordShirt = prod("옥스포드 셔츠", "유신사 스탠다드", 69000L, 셔츠);
        Product oversizedTee = prod("오버핏 티셔츠", "유신사 스탠다드", 24900L, 티셔츠);
        Product stripeTee = prod("스트라이프 티셔츠", "유신사 스탠다드", 29000L, 티셔츠);
        Product denimShirt = prod("데님 셔츠", "리바이스", 89000L, 셔츠);
        Product checkShirt = prod("체크 셔츠", "유니클로", 49000L, 셔츠);

        Product blueJeans = prod("블루 진", "유신사 스탠다드", 99000L, 청바지);
        Product skinnyJeans = prod("스키니 진", "유신사 스탠다드", 109000L, 청바지);
        Product blackSlacks = prod("블랙 슬랙스", "유신사 스탠다드", 69000L, 슬랙스);
        Product graySlacks = prod("그레이 슬랙스", "유신사 스탠다드", 79000L, 슬랙스);
        Product widePants = prod("와이드 팬츠", "무신사 스탠다드", 79000L, 슬랙스);
        Product cargoPants = prod("카고 팬츠", "디키즈", 89000L, 슬랙스);
        Product damageJeans = prod("데미지 진", "리바이스", 129000L, 청바지);


        Product leatherJacket = prod("가죽 자켓", "유신사 스탠다드", 89000L, 자켓);
        Product paddingJacket = prod("숏 패딩 자켓", "유신사 스탠다드", 129000L, 자켓);
        Product trenchCoat = prod("트렌치 코트", "유신사 스탠다드", 149000L, 코트);
        Product longCoat = prod("롱 울 코트", "유신사 스탠다드", 199000L, 코트);
        Product hoodieZipUp = prod("후드 집업", "나이키", 99000L, 자켓);
        Product fleeceJacket = prod("플리스 자켓", "노스페이스", 129000L, 자켓);
        Product shortCoat = prod("숏 코트", "자라", 159000L, 코트);

        Product airMax = prod("에어맥스 90", "나이키", 139000L, 스니커즈);
        Product canvas = prod("클래식 캔버스", "컨버스", 79000L, 스니커즈);
        Product suedeLafer = prod("수에드 로퍼", "타비", 189000L, 로퍼);
        Product pennyLafer = prod("페니 로퍼", "폴로", 149000L, 로퍼);
        Product runningShoes = prod("러닝화", "아디다스", 119000L, 스니커즈);
        Product highTop = prod("하이탑 스니커즈", "컨버스", 89000L, 스니커즈);
        Product tasselLoafer = prod("테슬 로퍼", "락포트", 179000L, 로퍼);

        productRepository.saveAll(List.of(
                whiteTee, blackTee, basicShirt, oxfordShirt,
                blueJeans, skinnyJeans, blackSlacks, graySlacks,
                leatherJacket, paddingJacket, trenchCoat, longCoat,
                airMax, canvas, suedeLafer, pennyLafer,
                oversizedTee, stripeTee, denimShirt, checkShirt,
                widePants, cargoPants, damageJeans,
                hoodieZipUp, fleeceJacket, shortCoat,
                runningShoes, highTop, tasselLoafer
        ));

        // ── 옵션 ──────────────────────────────────────────────────────
        optionRepository.saveAll(List.of(
                opt(whiteTee, "S", 80), opt(whiteTee, "M", 100), opt(whiteTee, "L", 70), opt(whiteTee, "XL", 50),
                opt(blackTee, "S", 80), opt(blackTee, "M", 100), opt(blackTee, "L", 70),
                opt(basicShirt, "S", 40), opt(basicShirt, "M", 50), opt(basicShirt, "L", 35),
                opt(oxfordShirt, "M", 40), opt(oxfordShirt, "L", 35), opt(oxfordShirt, "XL", 20),
                opt(oversizedTee, "M", 50), opt(oversizedTee, "L", 40), opt(oversizedTee, "XL", 30),
                opt(stripeTee, "M", 60), opt(stripeTee, "L", 50),
                opt(denimShirt, "M", 30), opt(denimShirt, "L", 25),
                opt(checkShirt, "M", 40), opt(checkShirt, "L", 35),

                opt(blueJeans, "30", 20), opt(blueJeans, "32", 50), opt(blueJeans, "34", 15),
                opt(skinnyJeans, "30", 25), opt(skinnyJeans, "32", 30), opt(skinnyJeans, "34", 20),
                opt(blackSlacks, "30", 25), opt(blackSlacks, "32", 30), opt(blackSlacks, "34", 20),
                opt(graySlacks, "30", 20), opt(graySlacks, "32", 25), opt(graySlacks, "34", 15),
                opt(widePants, "30", 25), opt(widePants, "32", 30),
                opt(cargoPants, "30", 20), opt(cargoPants, "32", 25),
                opt(damageJeans, "30", 15), opt(damageJeans, "32", 20),

                opt(leatherJacket, "S", 15), opt(leatherJacket, "M", 30), opt(leatherJacket, "L", 25), opt(leatherJacket, "XL", 10),
                opt(paddingJacket, "S", 20), opt(paddingJacket, "M", 30), opt(paddingJacket, "L", 25),
                opt(trenchCoat, "M", 20), opt(trenchCoat, "L", 15),
                opt(longCoat, "M", 15), opt(longCoat, "L", 10),
                opt(hoodieZipUp, "M", 40), opt(hoodieZipUp, "L", 35),
                opt(fleeceJacket, "M", 30), opt(fleeceJacket, "L", 25),
                opt(shortCoat, "M", 20), opt(shortCoat, "L", 15),

                opt(airMax, "250", 30), opt(airMax, "260", 40), opt(airMax, "270", 35), opt(airMax, "280", 20),
                opt(canvas, "250", 25), opt(canvas, "260", 30), opt(canvas, "270", 25),
                opt(suedeLafer, "250", 15), opt(suedeLafer, "260", 20), opt(suedeLafer, "270", 15),
                opt(pennyLafer, "255", 10), opt(pennyLafer, "265", 15), opt(pennyLafer, "275", 10),
                opt(runningShoes, "260", 30), opt(runningShoes, "270", 25),
                opt(highTop, "260", 20), opt(highTop, "270", 20),
                opt(tasselLoafer, "260", 15), opt(tasselLoafer, "270", 10)
        ));

        // ── 주문 & 배송 샘플 ───────────────────────────────────────────
        org.springframework.transaction.support.TransactionSynchronizationManager
                .isActualTransactionActive(); // flush 보장용 dummy

        Order order = orderRepository.save(Order.builder()
                .member(member)
                .receiverName("홍길동")
                .receiverPhone("010-2222-3333")
                .receiverAddress("서울특별시 종로구 청와대로 1")
                .status(OrderStatus.CREATED)
                .build());

        deliveryRepository.save(Delivery.builder()
                .order(order)
                .trackingNumber("001234")
                .deliveryStatus(DeliveryStatus.READY)
                .build());
    }

    private Category cat(Category parent, String name) {
        return Category.builder().parent(parent).name(name).build();
    }

    private Product prod(String name, String brand, Long price, Category category) {
        return Product.builder()
                .name(name).brandName(brand).price(price)
                .category(category).likeCount(0).clickCount(0)
                .build();
    }

    private ProductOption opt(Product product, String name, int stock) {
        return ProductOption.builder()
                .product(product).optionName(name).stock(stock)
                .build();
    }
}
