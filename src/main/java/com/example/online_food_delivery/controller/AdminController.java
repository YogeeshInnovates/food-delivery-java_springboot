package com.example.online_food_delivery.controller;

import com.example.online_food_delivery.dto.authdto.UserResponse;
import com.example.online_food_delivery.dto.order_dto.OrderResponse;
import com.example.online_food_delivery.dto.restaurantDto.RestaurantResponse;
import com.example.online_food_delivery.service.OrderService;
import com.example.online_food_delivery.service.RestaurantService;
import com.example.online_food_delivery.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;
    private final RestaurantService restaurantService;
    private final OrderService orderService;

    @GetMapping("/users")
    public ResponseEntity<Page<UserResponse>> listAllUsers(Pageable pageable) {
        return ResponseEntity.ok(userService.getAllUsers(pageable));
    }

    @PatchMapping("/users/{id}/block")
    public ResponseEntity<Void> toggleUserBlock(@PathVariable Long id) {
        userService.toggleUserStatus(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/restaurants")
    public ResponseEntity<List<RestaurantResponse>> viewAllRestaurants() {
        return ResponseEntity.ok(restaurantService.getAllRestaurants());
    }

    @PatchMapping("/restaurants/{id}/approve")
    public ResponseEntity<Void> approveRestaurant(@PathVariable Long id) {
        restaurantService.approveRestaurant(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/orders")
    public ResponseEntity<Page<OrderResponse>> viewAllOrders(Pageable pageable) {
        return ResponseEntity.ok(orderService.getAllOrders(pageable));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        return ResponseEntity.ok(orderService.getPlatformStats());
    }
}
