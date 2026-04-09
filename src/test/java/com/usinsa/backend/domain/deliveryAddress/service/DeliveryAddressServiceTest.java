package com.usinsa.backend.domain.deliveryAddress.service;

import com.usinsa.backend.domain.deliveryAddress.dto.DeliveryAddressDto;
import com.usinsa.backend.domain.deliveryAddress.entity.DeliveryAddress;
import com.usinsa.backend.domain.deliveryAddress.repository.DeliveryAddressRepository;
import com.usinsa.backend.domain.member.entity.Member;
import com.usinsa.backend.domain.member.repository.MemberRepository;
import jakarta.persistence.EntityNotFoundException;
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
class DeliveryAddressServiceTest {

    @InjectMocks
    private DeliveryAddressService deliveryAddressService;

    @Mock
    private DeliveryAddressRepository deliveryAddressRepository;

    @Mock
    private MemberRepository memberRepository;

    private Member testMember;
    private DeliveryAddress testDeliveryAddress;
    private DeliveryAddressDto.CreateReq createReq;

    @BeforeEach
    void setUp() {
        testMember = Member.builder()
                .id(1L)
                .usinaId("testuser")
                .name("테스트유저")
                .build();

        testDeliveryAddress = DeliveryAddress.builder()
                .id(1L)
                .member(testMember)
                .receiverName("홍길동")
                .receiverPhone("010-1234-5678")
                .receiverAddress("서울시 강남구")
                .build();

        createReq = DeliveryAddressDto.CreateReq.builder()
                .memberId(1L)
                .receiverName("홍길동")
                .receiverPhone("010-1234-5678")
                .receiverAddress("서울시 강남구")
                .build();
    }

    @Nested
    @DisplayName("배송지 생성 테스트")
    class CreateTest {

        @Test
        @DisplayName("정상적으로 배송지를 생성한다")
        void create_Success() {
            // given
            given(memberRepository.findById(anyLong())).willReturn(Optional.of(testMember));
            given(deliveryAddressRepository.save(any(DeliveryAddress.class))).willReturn(testDeliveryAddress);

            // when
            DeliveryAddressDto.Response result = deliveryAddressService.create(createReq);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getReceiverName()).isEqualTo("홍길동");
            assertThat(result.getReceiverAddress()).isEqualTo("서울시 강남구");
        }

        @Test
        @DisplayName("회원이 없으면 배송지 생성에 실패한다")
        void create_MemberNotFound_Fail() {
            // given
            given(memberRepository.findById(anyLong())).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> deliveryAddressService.create(createReq))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("회원이 존재하지 않습니다.");
        }
    }

    @Nested
    @DisplayName("배송지 조회 테스트")
    class FindTest {

        @Test
        @DisplayName("ID로 배송지를 조회한다")
        void findById_Success() {
            // given
            given(deliveryAddressRepository.findWithMemberById(anyLong()))
                    .willReturn(Optional.of(testDeliveryAddress));

            // when
            DeliveryAddressDto.Response result = deliveryAddressService.findById(1L);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getDeliveryAddressId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("존재하지 않는 배송지 조회 시 예외가 발생한다")
        void findById_NotFound_Fail() {
            // given
            given(deliveryAddressRepository.findWithMemberById(anyLong()))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> deliveryAddressService.findById(999L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("배송지가 존재하지 않습니다.");
        }

        @Test
        @DisplayName("모든 배송지를 조회한다")
        void findAll_Success() {
            // given
            DeliveryAddress address2 = DeliveryAddress.builder()
                    .id(2L)
                    .member(testMember)
                    .receiverName("김철수")
                    .receiverPhone("010-9876-5432")
                    .receiverAddress("서울시 서초구")
                    .build();

            given(deliveryAddressRepository.findAll())
                    .willReturn(Arrays.asList(testDeliveryAddress, address2));

            // when
            List<DeliveryAddressDto.Response> results = deliveryAddressService.findAll();

            // then
            assertThat(results).hasSize(2);
        }
    }

    @Nested
    @DisplayName("배송지 수정 테스트")
    class UpdateTest {

        @Test
        @DisplayName("정상적으로 배송지를 수정한다")
        void update_Success() {
            // given
            DeliveryAddressDto.UpdateReq updateReq = DeliveryAddressDto.UpdateReq.builder()
                    .receiverName("김영희")
                    .receiverPhone("010-5555-5555")
                    .receiverAddress("서울시 송파구")
                    .build();

            given(deliveryAddressRepository.findById(anyLong()))
                    .willReturn(Optional.of(testDeliveryAddress));

            // when
            DeliveryAddressDto.Response result = deliveryAddressService.update(1L, updateReq);

            // then
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("존재하지 않는 배송지 수정 시 예외가 발생한다")
        void update_NotFound_Fail() {
            // given
            DeliveryAddressDto.UpdateReq updateReq = DeliveryAddressDto.UpdateReq.builder().build();
            given(deliveryAddressRepository.findById(anyLong())).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> deliveryAddressService.update(999L, updateReq))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("배송지가 존재하지 않습니다.");
        }
    }

    @Nested
    @DisplayName("배송지 삭제 테스트")
    class DeleteTest {

        @Test
        @DisplayName("정상적으로 배송지를 삭제한다")
        void delete_Success() {
            // given
            given(deliveryAddressRepository.existsById(anyLong())).willReturn(true);
            doNothing().when(deliveryAddressRepository).deleteById(anyLong());

            // when
            deliveryAddressService.delete(1L);

            // then
            verify(deliveryAddressRepository).deleteById(1L);
        }

        @Test
        @DisplayName("존재하지 않는 배송지 삭제 시 예외가 발생한다")
        void delete_NotFound_Fail() {
            // given
            given(deliveryAddressRepository.existsById(anyLong())).willReturn(false);

            // when & then
            assertThatThrownBy(() -> deliveryAddressService.delete(999L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("배송지가 존재하지 않습니다.");
        }
    }
}
