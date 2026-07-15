package com.example.online_food_delivery.controller;

import com.example.online_food_delivery.dto.restaurantDto.RestaurantResponse;
import com.example.online_food_delivery.service.RestaurantService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/restaurants")
@RequiredArgsConstructor
public class RestaurantController {
    private final RestaurantService restaurantService;

    @GetMapping
    public ResponseEntity<Page<RestaurantResponse>> listRestaurants(Pageable pageable) {
        return ResponseEntity.ok(restaurantService.listRestaurant(pageable));
    }

    @GetMapping("/filter")
    public ResponseEntity<List<RestaurantResponse>> filterRestaurants(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String cuisine) {
        return ResponseEntity.ok(restaurantService.filterRestaurant(city, cuisine));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RestaurantResponse> getRestaurant(@PathVariable Long id) {
        return ResponseEntity.ok(restaurantService.getRestaurantPublic(id));
    }
}
