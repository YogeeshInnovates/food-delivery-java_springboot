package com.example.online_food_delivery.dto.authdto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistrationOtpData {
    private String otp;
    private String role;
    private String name;
    private String email;
    private String password;
    private String phoneNumber;
    private String address;
    private String restaurantName;
    private String licenseNumber;
    private String restaurantAddress;
}
