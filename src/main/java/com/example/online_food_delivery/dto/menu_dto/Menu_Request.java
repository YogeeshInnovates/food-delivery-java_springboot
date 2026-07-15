package com.example.online_food_delivery.dto.menu_dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Menu_Request {

    private String name;
    private Double price;
    private String category;
    private String description;
    private Boolean isVeg;

    private Long restaurantId;
}
