package com.example.online_food_delivery.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItem implements Serializable {
    private Long menuItemId;
    private String name;
    private Double price;
    private Integer quantity;
    private Long restaurantId;
}
