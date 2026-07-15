package com.example.online_food_delivery.controller;
import com.example.online_food_delivery.dto.order_dto.OrderResponse;
import com.example.online_food_delivery.dto.payment_dto.DeliveryAddressRequest;
import com.example.online_food_delivery.dto.payment_dto.PaymentRequest;
import com.example.online_food_delivery.model.OrderStatus;
import com.example.online_food_delivery.service.OrderService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<OrderResponse> placeOrder() {
        return new ResponseEntity<>(orderService.placeOrder(), HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Page<OrderResponse>> getMyOrders(Pageable pageable) {
        return ResponseEntity.ok(orderService.getMyOrders(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'OWNER', 'ADMIN')")
    public ResponseEntity<OrderResponse> getOrderDetails(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderDetails(id));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable Long id,
            @RequestParam OrderStatus status) {
        return ResponseEntity.ok(orderService.updateOrderStatus(id, status));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String reason = body.getOrDefault("reason", "Not specified");
        OrderResponse resp = orderService.cancelOrder(id, reason);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/{id}/pay")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<OrderResponse> payForOrder(
            @PathVariable Long id,
            @RequestBody PaymentRequest request) {
        return ResponseEntity.ok(orderService.payForOrder(id, request));
    }

    @PatchMapping("/{id}/delivery-address")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<OrderResponse> updateDeliveryAddress(
            @PathVariable Long id,
            @RequestBody DeliveryAddressRequest request) {
        return ResponseEntity.ok(orderService.updateDeliveryAddress(id, request));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Double> getMyTotalSpend() {
        return ResponseEntity.ok(orderService.getMyTotalSpend());
    }
}
