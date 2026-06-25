package com.pvc.foodie.feature.address.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pvc.foodie.comman.response.ApiResponse;
import com.pvc.foodie.feature.address.dto.AddressRequest;
import com.pvc.foodie.feature.address.dto.AddressResponse;
import com.pvc.foodie.feature.address.service.AddressService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    public ApiResponse<List<AddressResponse>> getAddresses() {
        return ApiResponse.ok(addressService.getAddresses());
    }

    @PostMapping
    public ApiResponse<AddressResponse> addAddress(@Valid @RequestBody AddressRequest request) {
        return ApiResponse.ok(addressService.addAddress(request));
    }

    @PatchMapping("/{id}")
    public ApiResponse<AddressResponse> updateAddress(@PathVariable UUID id,
            @Valid @RequestBody AddressRequest request) {
        return ApiResponse.ok(addressService.updateAddress(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteAddress(@PathVariable UUID id) {
        addressService.deleteAddress(id);
        return ApiResponse.ok("Address deleted successfully");
    }
}
