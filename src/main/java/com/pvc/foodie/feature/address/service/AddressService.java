package com.pvc.foodie.feature.address.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pvc.foodie.comman.exception.BusinessException;
import com.pvc.foodie.comman.exception.ErrorCode;
import com.pvc.foodie.feature.address.dto.AddressRequest;
import com.pvc.foodie.feature.address.dto.AddressResponse;
import com.pvc.foodie.feature.address.entity.Address;
import com.pvc.foodie.feature.address.repository.AddressRepository;
import com.pvc.foodie.feature.auth.entity.Role;
import com.pvc.foodie.feature.auth.entity.User;
import com.pvc.foodie.security.CurrentUserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;
    private final CurrentUserService currentUserService;

    public List<AddressResponse> getAddresses() {
        User user = currentUserService.getCurrentUser();
        requireCustomer(user);
        List<AddressResponse> addresses = addressRepository.findByUserId(user.getId()).stream()
                .map(this::toResponse)
                .toList();
        log.info("Addresses fetched: userId={}, count={}", user.getId(), addresses.size());
        return addresses;
    }

    @Transactional
    public AddressResponse addAddress(AddressRequest request) {
        User user = currentUserService.getCurrentUser();
        requireCustomer(user);
        Address address = new Address();
        address.setUser(user);
        apply(address, request);
        Address saved = addressRepository.save(address);
        log.info("Address added: userId={}, addressId={}, city={}, defaultAddress={}",
                user.getId(), saved.getId(), saved.getCity(), saved.isDefaultAddress());
        return toResponse(saved);
    }

    @Transactional
    public AddressResponse updateAddress(UUID id, AddressRequest request) {
        User user = currentUserService.getCurrentUser();
        requireCustomer(user);
        Address address = addressRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Address not found"));
        apply(address, request);
        log.info("Address updated: userId={}, addressId={}, city={}, defaultAddress={}",
                user.getId(), id, address.getCity(), address.isDefaultAddress());
        return toResponse(address);
    }

    @Transactional
    public void deleteAddress(UUID id) {
        User user = currentUserService.getCurrentUser();
        requireCustomer(user);
        Address address = addressRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Address not found"));
        addressRepository.delete(address);
        log.info("Address deleted: userId={}, addressId={}", user.getId(), id);
    }

    private void apply(Address address, AddressRequest request) {
        address.setTitle(request.title());
        address.setAddressLine1(request.addressLine1());
        address.setAddressLine2(request.addressLine2());
        address.setCity(request.city());
        address.setState(request.state());
        address.setLatitude(request.latitude());
        address.setLongitude(request.longitude());
        address.setDefaultAddress(request.defaultAddress());
    }

    private AddressResponse toResponse(Address address) {
        return new AddressResponse(
                address.getId(),
                address.getTitle(),
                address.getAddressLine1(),
                address.getAddressLine2(),
                address.getCity(),
                address.getState(),
                address.getLatitude(),
                address.getLongitude(),
                address.isDefaultAddress());
    }

    private void requireCustomer(User user) {
        if (user.getRole() != Role.CUSTOMER && user.getRole() != Role.ADMIN) {
            log.warn("Address access denied: userId={}, role={}", user.getId(), user.getRole());
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "Customer access required");
        }
    }
}
