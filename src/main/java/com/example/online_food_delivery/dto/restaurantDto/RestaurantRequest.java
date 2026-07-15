package com.example.online_food_delivery.dto.restaurantDto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RestaurantRequest {
    @NotBlank
    private String name;

    @NotBlank
    private String cuisineType;

    private String description;

    @NotBlank
    private String address;

    private String phoneNumber;
    private String openingTime;
    private String closingTime;
    private String city;
}
