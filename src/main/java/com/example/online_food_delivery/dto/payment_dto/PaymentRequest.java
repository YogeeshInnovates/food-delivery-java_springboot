package com.example.online_food_delivery.dto.payment_dto;

import com.example.online_food_delivery.model.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {
    private PaymentMethod paymentMethod;
}
