package com.example.online_food_delivery.dto.authdto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRequest {
    @NotBlank
    private String name;

    @NotBlank
    @Email(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", message = "Invalid email format")
    private String email;

    @NotBlank
    private String password;

    @NotBlank
    @Pattern(regexp = "\\d{10}", message = "Phone number must be exactly 10 digits")
    private String phoneNumber;

    @NotBlank
    private  String address;
}
