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
public class DeliveryAddressService {

    private final DeliveryAddressRepository deliveryAddressRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public DeliveryAddressDto.Res create(DeliveryAddressDto.CreateReq request) {
        Member member = memberRepository.findById(request.getMemberId())
                .orElseThrow(() -> new EntityNotFoundException("회원이 존재하지 않습니다."));

        DeliveryAddress deliveryAddress = deliveryAddressRepository.save(request.toEntity(member));
        return DeliveryAddressDto.Res.fromEntity(deliveryAddress);
    }

    @Transactional(readOnly = true)
    public DeliveryAddressDto.Res findById(Long id) {
        DeliveryAddress deliveryAddress = deliveryAddressRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("배송지가 존재하지 않습니다."));
        return DeliveryAddressDto.Res.fromEntity(deliveryAddress);
    }

    @Transactional(readOnly = true)
    public List<DeliveryAddressDto.Res> findAll() {
        return deliveryAddressRepository.findAll().stream()
                .map(DeliveryAddressDto.Res::fromEntity)
                .toList();
    }

    @Transactional
    public DeliveryAddressDto.Res update(Long id, DeliveryAddressDto.UpdateReq request) {
        DeliveryAddress deliveryAddress = deliveryAddressRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("배송지가 존재하지 않습니다."));

        deliveryAddress.update(request.getReceiverName(), request.getReceiverPhone(), request.getReceiverAddress());
        return DeliveryAddressDto.Res.fromEntity(deliveryAddress);
    }

    @Transactional
    public void delete(Long id) {
        if (!deliveryAddressRepository.existsById(id)) {
            throw new EntityNotFoundException("배송지가 존재하지 않습니다.");
        }
        deliveryAddressRepository.deleteById(id);
    }
}