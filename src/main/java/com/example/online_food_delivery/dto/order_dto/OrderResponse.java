package com.example.online_food_delivery.dto.order_dto;

import com.example.online_food_delivery.model.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private Long id;
    private String restaurantName;
    private OrderStatus status;
    private Double totalAmount;
    private List<OrderItemResponse> items;
    private LocalDateTime placedAt;
}
