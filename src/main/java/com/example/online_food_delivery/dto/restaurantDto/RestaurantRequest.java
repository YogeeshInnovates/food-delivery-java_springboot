package com.example.online_food_delivery.dto.restaurantDto;

import jakarta.persistence.Column;
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
    private String city;

    @NotBlank
    private String cuisine;

}
