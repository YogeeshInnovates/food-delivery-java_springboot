package com.example.online_food_delivery.dto.order_dto;

import com.example.online_food_delivery.model.OrderStatus;
import com.example.online_food_delivery.model.PaymentMethod;
import com.example.online_food_delivery.model.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private Long id;
    private String restaurantName;
    private String customerName;
    private OrderStatus status;
    private PaymentStatus paymentStatus;
    private PaymentMethod paymentMethod;
    private Double totalAmount;
    private String deliveryAddress;
    private Double deliveryLatitude;
    private Double deliveryLongitude;
    private Double restaurantLatitude;
    private Double restaurantLongitude;
    private String cancellationReason;
    private Double refundAmount;
    private List<OrderItemResponse> items;
    private ZonedDateTime placedAt;
}
