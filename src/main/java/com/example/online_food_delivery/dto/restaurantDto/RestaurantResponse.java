package com.example.online_food_delivery.dto.restaurantDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantResponse {
    private Long id;
    private String name;
    private String city;
    private Double rating;
    private String status;
    private String cuisine;
    private LocalDateTime createdAt;
    private String ownerName;
}
