package com.pvc.foodie.feature.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CustomerSignupRequest {

    @Email
    @NotBlank
    @Size(max = 255)
    private String email;

    @NotBlank
    @Size(min = 8, max = 100)
    private String password;

    @NotBlank
    @Size(max = 150)
    private String name;

    @NotBlank
    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "must be a valid phone number")
    private String phone;

    @Size(max = 255)
    private String razorpayLinkedAccountId;
}
