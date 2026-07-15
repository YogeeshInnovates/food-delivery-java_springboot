package com.example.online_food_delivery.dto.restaurantDto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompleteRegistrationRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String cuisineType;

    @NotBlank
    private String licenseNumber;

    private String description;

    @NotBlank
    private String address;

    private String phoneNumber;
    private String openingTime;
    private String closingTime;
    private String city;

    @NotBlank
    private String restaurantImageUrl;

    @NotNull
    @Size(min = 1)
    @Valid
    private List<MenuItemEntry> menuItems;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MenuItemEntry {
        @NotBlank
        private String name;

        @NotNull
        private Double price;

        @NotBlank
        private String category;

        private String description;

        private Boolean isVeg;
        private Boolean isAvailable;

        @NotBlank
        private String imageUrl;
    }
}
