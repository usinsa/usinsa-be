package com.usinsa.backend.domain.delivery.service;

import com.usinsa.backend.domain.delivery.dto.DeliveryDto;
import com.usinsa.backend.domain.delivery.entity.Delivery;
import com.usinsa.backend.domain.delivery.entity.DeliveryStatus;
import com.usinsa.backend.domain.delivery.repository.DeliveryRepository;
import com.usinsa.backend.domain.member.entity.Member;
import com.usinsa.backend.domain.order.entity.Order;
import com.usinsa.backend.domain.order.entity.OrderStatus;
import com.usinsa.backend.domain.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceTest {

    @InjectMocks
    private DeliveryService deliveryService;

    @Mock
    private DeliveryRepository deliveryRepository;

    @Mock
    private OrderRepository orderRepository;

    private Order testOrder;
    private Delivery testDelivery;
    private DeliveryDto.CreateReq createReq;

    @BeforeEach
    void setUp() {
        Member testMember = Member.builder()
                .id(1L)
                .usinaId("testuser")
                .build();

        testOrder = Order.builder()
                .id(1L)
                .member(testMember)
                .receiverAddress("서울시 강남구")
                .receiverName("홍길동")
                .receiverPhone("010-1234-5678")
                .status(OrderStatus.CREATED)
                .build();

        testDelivery = Delivery.builder()
                .id(1L)
                .order(testOrder)
                .trackingNumber("1234567890")
                .deliveryStatus(DeliveryStatus.READY)
                .build();

        createReq = DeliveryDto.CreateReq.builder()
                .orderId(1L)
                .trackingNumber("1234567890")
                .deliveryStatus(DeliveryStatus.READY)
                .build();
    }

    @Nested
    @DisplayName("배송 생성 테스트")
    class CreateTest {

        @Test
        @DisplayName("정상적으로 배송 정보를 생성한다")
        void create_Success() {
            // given
            given(orderRepository.findById(anyLong())).willReturn(Optional.of(testOrder));
            given(deliveryRepository.save(any(Delivery.class))).willReturn(testDelivery);

            // when
            DeliveryDto.Response result = deliveryService.create(createReq);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getTrackingNumber()).isEqualTo("1234567890");
            assertThat(result.getDeliveryStatus()).isEqualTo(DeliveryStatus.READY);
        }

        @Test
        @DisplayName("주문이 없으면 배송 생성에 실패한다")
        void create_OrderNotFound_Fail() {
            // given
            given(orderRepository.findById(anyLong())).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> deliveryService.create(createReq))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("주문을 찾을 수 없습니다.");
        }
    }

    @Nested
    @DisplayName("배송 조회 테스트")
    class FindTest {

        @Test
        @DisplayName("ID로 배송을 조회한다")
        void findById_Success() {
            // given
            given(deliveryRepository.findWithOrderById(anyLong()))
                    .willReturn(Optional.of(testDelivery));

            // when
            DeliveryDto.Response result = deliveryService.findById(1L);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getTrackingNumber()).isEqualTo("1234567890");
        }

        @Test
        @DisplayName("존재하지 않는 배송 조회 시 예외가 발생한다")
        void findById_NotFound_Fail() {
            // given
            given(deliveryRepository.findWithOrderById(anyLong()))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> deliveryService.findById(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("배송 정보를 찾을 수 없습니다.");
        }

        @Test
        @DisplayName("모든 배송을 조회한다")
        void findAll_Success() {
            // given
            Delivery delivery2 = Delivery.builder()
                    .id(2L)
                    .order(testOrder)
                    .trackingNumber("9876543210")
                    .deliveryStatus(DeliveryStatus.IN_TRANSIT)
                    .build();

            given(deliveryRepository.findAll()).willReturn(Arrays.asList(testDelivery, delivery2));

            // when
            List<DeliveryDto.Response> results = deliveryService.findAll();

            // then
            assertThat(results).hasSize(2);
        }
    }

    @Nested
    @DisplayName("배송 수정 테스트")
    class UpdateTest {

        @Test
        @DisplayName("정상적으로 배송 정보를 수정한다")
        void update_Success() {
            // given
            DeliveryDto.CreateReq updateReq = DeliveryDto.CreateReq.builder()
                    .trackingNumber("0000000000")
                    .deliveryStatus(DeliveryStatus.IN_TRANSIT)
                    .build();

            given(deliveryRepository.findById(anyLong())).willReturn(Optional.of(testDelivery));

            // when
            DeliveryDto.Response result = deliveryService.update(1L, updateReq);

            // then
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("존재하지 않는 배송 수정 시 예외가 발생한다")
        void update_NotFound_Fail() {
            // given
            DeliveryDto.CreateReq updateReq = DeliveryDto.CreateReq.builder().build();
            given(deliveryRepository.findById(anyLong())).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> deliveryService.update(999L, updateReq))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("배송 정보를 찾을 수 없습니다.");
        }
    }

    @Nested
    @DisplayName("배송 삭제 테스트")
    class DeleteTest {

        @Test
        @DisplayName("정상적으로 배송 정보를 삭제한다")
        void delete_Success() {
            // given
            given(deliveryRepository.findById(anyLong())).willReturn(Optional.of(testDelivery));
            doNothing().when(deliveryRepository).delete(any(Delivery.class));

            // when
            deliveryService.delete(1L);

            // then
            verify(deliveryRepository).delete(testDelivery);
        }

        @Test
        @DisplayName("존재하지 않는 배송 삭제 시 예외가 발생한다")
        void delete_NotFound_Fail() {
            // given
            given(deliveryRepository.findById(anyLong())).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> deliveryService.delete(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("배송 정보를 찾을 수 없습니다.");
        }
    }
}
