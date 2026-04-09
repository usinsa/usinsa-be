package com.usinsa.backend.domain.search.elastic.init;

import com.usinsa.backend.domain.product.repository.ProductRepository;
import com.usinsa.backend.domain.search.port.ProductIndexPort;
import com.usinsa.backend.domain.search.result.dto.ProductSearchDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 앱 시작 시 검색 인덱스 초기화 (ES or ZincSearch 공통)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductIndexer implements CommandLineRunner {

    private final ProductRepository productRepository;
    private final ProductIndexPort productIndexPort;

    @Override
    public void run(String... args) {
        long existing = productIndexPort.count();
        if (existing > 0) {
            log.info("검색 인덱스에 이미 {}개 상품이 있습니다. 초기화를 건너뜁니다.", existing);
            return;
        }

        log.info("검색 인덱스 초기화 시작...");
        List<ProductSearchDto> docs = productRepository.findAll().stream()
                .map(p -> ProductSearchDto.builder()
                        .id(p.getId())
                        .name(p.getName())
                        .brandName(p.getBrandName())
                        .categoryName(p.getCategory().getName())
                        .price(p.getPrice())
                        .likeCount(p.getLikeCount())
                        .clickCount(p.getClickCount())
                        .build())
                .toList();

        productIndexPort.saveAll(docs);
        log.info("검색 인덱스 초기화 완료 ({} 개 상품)", docs.size());
    }
}
