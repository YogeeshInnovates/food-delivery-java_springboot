package com.example.online_food_delivery.controller;

import com.example.online_food_delivery.dto.restaurantDto.RestaurantRequest;
import com.example.online_food_delivery.dto.restaurantDto.RestaurantResponse;
import com.example.online_food_delivery.service.RestaurantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
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
        return ResponseEntity.ok(restaurantService.getRestaurant(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<RestaurantResponse> addRestaurant(@Valid @RequestBody RestaurantRequest restaurantData) {
        return ResponseEntity.ok(restaurantService.addRestaurent(restaurantData));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<RestaurantResponse> updateRestaurant(
            @PathVariable Long id,
            @RequestBody RestaurantRequest req) {
        return ResponseEntity.ok(restaurantService.updateRestaurant(id, req));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<RestaurantResponse> toggleStatus(@PathVariable Long id) {
        return ResponseEntity.ok(restaurantService.update_Status(id));
    }
}
