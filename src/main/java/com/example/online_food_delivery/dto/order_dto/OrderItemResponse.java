package com.example.online_food_delivery.dto.order_dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponse {
    private Long menuItemId;
    private String name;
    private Integer quantity;
    private Double priceAtOrder;
}
