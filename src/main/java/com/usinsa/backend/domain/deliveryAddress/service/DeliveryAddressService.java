package com.usinsa.backend.domain.deliveryAddress.service;

import com.usinsa.backend.domain.deliveryAddress.dto.DeliveryAddressDto;
import com.usinsa.backend.domain.deliveryAddress.entity.DeliveryAddress;
import com.usinsa.backend.domain.deliveryAddress.repository.DeliveryAddressRepository;
import com.usinsa.backend.domain.member.entity.Member;
import com.usinsa.backend.domain.member.repository.MemberRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class DeliveryAddressService {

    private final DeliveryAddressRepository deliveryAddressRepository;
    private final MemberRepository memberRepository;

    public DeliveryAddressDto.Response create(DeliveryAddressDto.CreateReq request) {
        Member member = memberRepository.findById(request.getMemberId())
                .orElseThrow(() -> new EntityNotFoundException("회원이 존재하지 않습니다."));

        DeliveryAddress deliveryAddress = toEntity(request, member);
        DeliveryAddress saved = deliveryAddressRepository.save(deliveryAddress);
        return toResDto(saved);
    }

    @Transactional(readOnly = true)
    public DeliveryAddressDto.Response findById(Long id) {
        DeliveryAddress deliveryAddress = deliveryAddressRepository.findWithMemberById(id)
                .orElseThrow(() -> new EntityNotFoundException("배송지가 존재하지 않습니다."));
        return toResDto(deliveryAddress);
    }

    @Transactional(readOnly = true)
    public List<DeliveryAddressDto.Response> findAll() {
        return deliveryAddressRepository.findAll().stream()
                .map(this::toResDto)
                .toList();
    }

    public DeliveryAddressDto.Response update(Long id, DeliveryAddressDto.UpdateReq request) {
        DeliveryAddress deliveryAddress = deliveryAddressRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("배송지가 존재하지 않습니다."));

        deliveryAddress.update(request.getReceiverName(), request.getReceiverPhone(), request.getReceiverAddress());
        return toResDto(deliveryAddress);
    }

    public void delete(Long id) {
        if (!deliveryAddressRepository.existsById(id)) {
            throw new EntityNotFoundException("배송지가 존재하지 않습니다.");
        }
        deliveryAddressRepository.deleteById(id);
    }

    private DeliveryAddress toEntity(DeliveryAddressDto.CreateReq request, Member member) {
        return DeliveryAddress.builder()
                .member(member)
                .receiverName(request.getReceiverName())
                .receiverPhone(request.getReceiverPhone())
                .receiverAddress(request.getReceiverAddress())
                .build();
    }

    private DeliveryAddressDto.Response toResDto(DeliveryAddress deliveryAddress) {
        return DeliveryAddressDto.Response.builder()
                .deliveryAddressId(deliveryAddress.getId())
                .memberId(deliveryAddress.getMember().getId())
                .receiverName(deliveryAddress.getReceiverName())
                .receiverPhone(deliveryAddress.getReceiverPhone())
                .receiverAddress(deliveryAddress.getReceiverAddress())
                .build();
    }

}