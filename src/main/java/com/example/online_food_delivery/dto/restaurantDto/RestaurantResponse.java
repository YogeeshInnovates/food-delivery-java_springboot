package com.example.online_food_delivery.dto.restaurantDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RestaurantResponse {
    private Long id;
    private String name;
    private String cuisineType;
    private String description;
    private String address;
    private String phoneNumber;
    private String openingTime;
    private String closingTime;
    private String imageUrl;
    private String licenseNumber;
    private Double rating;
    private Boolean isActive;
    private String status;
    private LocalDateTime createdAt;
    private String ownerName;
    private String ownerEmail;
    private String city;
    private Double latitude;
    private Double longitude;
}
