package com.example.online_food_delivery.dto.menu_dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Menu_Response {
    private Long id;
    private String name;
    private Double price;
    private String category;
    private String description;

    private String status;

    private Long restaurantId;
    private String restaurantName;

    private LocalDateTime createdAt;
}
