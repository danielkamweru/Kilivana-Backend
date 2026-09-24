package com.kilivana.backend.admin.service;

import com.kilivana.backend.admin.dto.AddressRequest;
import com.kilivana.backend.admin.dto.AddressResponse;
import com.kilivana.backend.admin.entity.Address;
import com.kilivana.backend.admin.repository.AddressRepository;
import com.kilivana.backend.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;

    public List<AddressResponse> getAddressesByUser(Long userId) {
        return addressRepository.findByUserId(userId).stream()
                .map(AddressResponse::fromEntity)
                .toList();
    }

    @Transactional
    public AddressResponse createAddress(Long userId, AddressRequest request) {
        Address address = Address.builder()
                .userId(userId)
                .addressText(request.getAddressText())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .label(request.getLabel())
                .updatedAt(LocalDateTime.now())
                .build();

        return AddressResponse.fromEntity(addressRepository.save(address));
    }

    @Transactional
    public AddressResponse updateAddress(Long userId, Long addressId, AddressRequest request) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", addressId));
        if (!address.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Address", addressId);
        }

        address.setAddressText(request.getAddressText());
        address.setLatitude(request.getLatitude());
        address.setLongitude(request.getLongitude());
        address.setLabel(request.getLabel());
        address.setUpdatedAt(LocalDateTime.now());

        return AddressResponse.fromEntity(addressRepository.save(address));
    }

    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", addressId));
        if (!address.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Address", addressId);
        }
        addressRepository.delete(address);
    }
}
