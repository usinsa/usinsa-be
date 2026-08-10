package com.usinsa.backend.domain.order.service;

import com.usinsa.backend.domain.member.entity.Member;
import com.usinsa.backend.domain.member.repository.MemberRepository;
import com.usinsa.backend.domain.order.dto.OrderDto;
import com.usinsa.backend.domain.order.entity.Order;
import com.usinsa.backend.domain.order.entity.OrderStatus;
import com.usinsa.backend.domain.order.repository.OrderRepository;
import com.usinsa.backend.global.exception.CustomException;
import com.usinsa.backend.global.exception.ErrorCode;
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
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @InjectMocks
    private OrderService orderService;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private MemberRepository memberRepository;

    private Member testMember;
    private Order testOrder;
    private OrderDto.CreateReq createReq;

    @BeforeEach
    void setUp() {
        testMember = Member.builder()
                .id(1L)
                .usinaId("testuser")
                .name("테스트유저")
                .build();

        testOrder = Order.builder()
                .id(1L)
                .member(testMember)
                .receiverAddress("서울시 강남구")
                .receiverName("홍길동")
                .receiverPhone("010-1234-5678")
                .status(OrderStatus.CREATED)
                .build();

        createReq = OrderDto.CreateReq.builder()
                .memberId(1L)
                .receiverAddress("서울시 강남구")
                .receiverName("홍길동")
                .receiverPhone("010-1234-5678")
                .build();
    }

    @Nested
    @DisplayName("주문 생성 테스트")
    class CreateTest {

        @Test
        @DisplayName("정상적으로 주문을 생성한다")
        void create_Success() {
            // given
            given(memberRepository.findById(anyLong())).willReturn(Optional.of(testMember));
            given(orderRepository.save(any(Order.class))).willReturn(testOrder);

            // when
            OrderDto.Response result = orderService.create(createReq);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getReceiverName()).isEqualTo("홍길동");
            assertThat(result.getStatus()).isEqualTo(OrderStatus.CREATED);
            verify(orderRepository).save(any(Order.class));
        }

        @Test
        @DisplayName("회원이 없으면 주문 생성에 실패한다")
        void create_MemberNotFound_Fail() {
            // given
            given(memberRepository.findById(anyLong())).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> orderService.create(createReq))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.MEMBER_NOT_FOUND.getMessage());
        }
    }

    @Nested
    @DisplayName("주문 조회 테스트")
    class FindTest {

        @Test
        @DisplayName("ID로 주문을 조회한다")
        void findById_Success() {
            // given
            given(orderRepository.findWithMemberAndDeliveryById(anyLong()))
                    .willReturn(Optional.of(testOrder));

            // when
            OrderDto.Response result = orderService.findById(1L);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getReceiverAddress()).isEqualTo("서울시 강남구");
        }

        @Test
        @DisplayName("존재하지 않는 주문 조회 시 예외가 발생한다")
        void findById_NotFound_Fail() {
            // given
            given(orderRepository.findWithMemberAndDeliveryById(anyLong()))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> orderService.findById(999L))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.ORDER_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("모든 주문을 조회한다")
        void findAll_Success() {
            // given
            Order order2 = Order.builder()
                    .id(2L)
                    .member(testMember)
                    .receiverAddress("서울시 서초구")
                    .receiverName("김철수")
                    .receiverPhone("010-9876-5432")
                    .status(OrderStatus.CREATED)
                    .build();

            given(orderRepository.findAll()).willReturn(Arrays.asList(testOrder, order2));

            // when
            List<OrderDto.Response> results = orderService.findAll();

            // then
            assertThat(results).hasSize(2);
        }
    }

    @Nested
    @DisplayName("주문 수정 테스트")
    class UpdateTest {

        @Test
        @DisplayName("정상적으로 주문을 수정한다")
        void update_Success() {
            // given
            OrderDto.UpdateReq updateReq = OrderDto.UpdateReq.builder()
                    .receiverAddress("서울시 서초구")
                    .receiverName("김영희")
                    .receiverPhone("010-5555-5555")
                    .build();

            given(orderRepository.findWithMemberAndDeliveryById(anyLong()))
                    .willReturn(Optional.of(testOrder));

            // when
            OrderDto.Response result = orderService.update(1L, updateReq);

            // then
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("존재하지 않는 주문 수정 시 예외가 발생한다")
        void update_NotFound_Fail() {
            // given
            OrderDto.UpdateReq updateReq = OrderDto.UpdateReq.builder().build();
            given(orderRepository.findWithMemberAndDeliveryById(anyLong()))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> orderService.update(999L, updateReq))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.ORDER_NOT_FOUND.getMessage());
        }
    }

    @Nested
    @DisplayName("주문 취소 테스트")
    class CancelTest {

        @Test
        @DisplayName("정상적으로 주문을 취소한다")
        void cancel_Success() {
            // given
            given(orderRepository.findWithMemberAndDeliveryById(anyLong()))
                    .willReturn(Optional.of(testOrder));

            // when
            OrderDto.Response result = orderService.cancel(1L);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        }

        @Test
        @DisplayName("존재하지 않는 주문 취소 시 예외가 발생한다")
        void cancel_NotFound_Fail() {
            // given
            given(orderRepository.findWithMemberAndDeliveryById(anyLong()))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> orderService.cancel(999L))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.ORDER_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("이미 취소된 주문을 다시 취소하면 예외가 발생한다")
        void cancel_AlreadyCancelled_Fail() {
            // given
            Order cancelledOrder = Order.builder()
                    .id(1L)
                    .member(testMember)
                    .receiverAddress("서울시 강남구")
                    .receiverName("홍길동")
                    .receiverPhone("010-1234-5678")
                    .status(OrderStatus.CANCELLED)
                    .build();

            given(orderRepository.findWithMemberAndDeliveryById(anyLong()))
                    .willReturn(Optional.of(cancelledOrder));

            // when & then
            assertThatThrownBy(() -> orderService.cancel(1L))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.ORDER_ALREADY_CANCELLED.getMessage());
        }

        @Test
        @DisplayName("결제가 완료된 주문은 취소할 수 없다")
        void cancel_CannotCancel_Fail() {
            // given
            Order completedOrder = Order.builder()
                    .id(1L)
                    .member(testMember)
                    .receiverAddress("서울시 강남구")
                    .receiverName("홍길동")
                    .receiverPhone("010-1234-5678")
                    .status(OrderStatus.PAYMENT_COMPLETED)
                    .build();

            given(orderRepository.findWithMemberAndDeliveryById(anyLong()))
                    .willReturn(Optional.of(completedOrder));

            // when & then
            assertThatThrownBy(() -> orderService.cancel(1L))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.ORDER_CANNOT_CANCEL.getMessage());
        }
    }

    @Nested
    @DisplayName("주문 상태 전이 테스트")
    class StatusTransitionTest {

        @Test
        @DisplayName("결제 준비 상태로 정상 전이한다")
        void updateToPaymentReady_Success() {
            // given
            given(orderRepository.findById(anyLong())).willReturn(Optional.of(testOrder));

            // when
            orderService.updateToPaymentReady(1L);

            // then
            assertThat(testOrder.getStatus()).isEqualTo(OrderStatus.PAYMENT_READY);
        }

        @Test
        @DisplayName("CREATED 상태가 아니면 결제 준비로 전이할 수 없다")
        void updateToPaymentReady_InvalidState_Fail() {
            // given
            testOrder.setStatus(OrderStatus.PAYMENT_READY);
            given(orderRepository.findById(anyLong())).willReturn(Optional.of(testOrder));

            // when & then
            assertThatThrownBy(() -> orderService.updateToPaymentReady(1L))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_INPUT_VALUE.getMessage());
        }

        @Test
        @DisplayName("결제 완료 상태로 정상 전이한다")
        void updateToPaymentCompleted_Success() {
            // given
            testOrder.setStatus(OrderStatus.PAYMENT_READY);
            given(orderRepository.findById(anyLong())).willReturn(Optional.of(testOrder));

            // when
            orderService.updateToPaymentCompleted(1L);

            // then
            assertThat(testOrder.getStatus()).isEqualTo(OrderStatus.PAYMENT_COMPLETED);
        }

        @Test
        @DisplayName("PAYMENT_READY 상태가 아니면 결제 완료로 전이할 수 없다")
        void updateToPaymentCompleted_InvalidState_Fail() {
            // given
            given(orderRepository.findById(anyLong())).willReturn(Optional.of(testOrder));

            // when & then
            assertThatThrownBy(() -> orderService.updateToPaymentCompleted(1L))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_INPUT_VALUE.getMessage());
        }

        @Test
        @DisplayName("결제 취소로 주문 상태를 취소로 변경한다")
        void updateToCancelled_Success() {
            // given
            given(orderRepository.findById(anyLong())).willReturn(Optional.of(testOrder));

            // when
            orderService.updateToCancelled(1L);

            // then
            assertThat(testOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        }

        @Test
        @DisplayName("존재하지 않는 주문의 상태 변경 시 예외가 발생한다")
        void updateToPaymentReady_NotFound_Fail() {
            // given
            given(orderRepository.findById(anyLong())).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> orderService.updateToPaymentReady(999L))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.ORDER_NOT_FOUND.getMessage());
        }
    }
}
