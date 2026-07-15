package com.example.online_food_delivery.dto.payment_dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryAddressRequest {
    private String deliveryAddress;
    private Double deliveryLatitude;
    private Double deliveryLongitude;
}
