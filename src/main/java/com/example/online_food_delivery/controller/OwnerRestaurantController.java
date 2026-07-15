package com.example.online_food_delivery.controller;

import com.example.online_food_delivery.dto.restaurantDto.CompleteRegistrationRequest;
import com.example.online_food_delivery.dto.restaurantDto.RestaurantRequest;
import com.example.online_food_delivery.dto.restaurantDto.RestaurantResponse;
import com.example.online_food_delivery.service.CloudinaryService;
import com.example.online_food_delivery.service.RestaurantService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.List;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/owner/restaurants")
@PreAuthorize("hasRole('OWNER')")
@RequiredArgsConstructor
public class OwnerRestaurantController {

    private final RestaurantService restaurantService;
    private final CloudinaryService cloudinaryService;

    @PostMapping
    public ResponseEntity<RestaurantResponse> createRestaurant(@Valid @RequestBody RestaurantRequest request) {
        return new ResponseEntity<>(restaurantService.addRestaurent(request), HttpStatus.CREATED);
    }

    @GetMapping("/my")
    public ResponseEntity<List<RestaurantResponse>> getMyRestaurants() {
        return ResponseEntity.ok(restaurantService.getMyRestaurants());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RestaurantResponse> getMyRestaurant(@PathVariable Long id) {
        return ResponseEntity.ok(restaurantService.getMyRestaurantById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RestaurantResponse> updateRestaurant(
            @PathVariable Long id,
            @Valid @RequestBody RestaurantRequest request) {
        return ResponseEntity.ok(restaurantService.updateRestaurant(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<RestaurantResponse> toggleStatus(@PathVariable Long id) {
        return ResponseEntity.ok(restaurantService.update_Status(id));
    }

    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RestaurantResponse> uploadImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        String imageUrl = cloudinaryService.uploadImage(file, "restaurants/" + id);
        return ResponseEntity.ok(restaurantService.updateRestaurantImage(id, imageUrl));
    }

    @PutMapping("/{id}/complete-registration")
    public ResponseEntity<RestaurantResponse> completeRegistration(
            @PathVariable Long id,
            @Valid @RequestBody CompleteRegistrationRequest request) {
        return ResponseEntity.ok(restaurantService.completeRegistration(id, request));
    }

    @PostMapping(value = "/{restaurantId}/menu-images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadMenuImage(
            @PathVariable Long restaurantId,
            @RequestParam("file") MultipartFile file) {
        String imageUrl = cloudinaryService.uploadImage(file, "menu-items/" + restaurantId + "/temp");
        return ResponseEntity.ok(Map.of("imageUrl", imageUrl));
    }

    @DeleteMapping("/{restaurantId}/menu-images")
    public ResponseEntity<Void> deleteMenuImage(@RequestParam String imageUrl) {
        cloudinaryService.deleteImage(imageUrl);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/image")
    public ResponseEntity<Void> deleteRestaurantImage(
            @PathVariable Long id,
            @RequestParam(required = false) String imageUrl) {
        if (imageUrl != null && !imageUrl.isBlank()) {
            cloudinaryService.deleteImage(imageUrl);
        }
        restaurantService.removeRestaurantImage(id);
        return ResponseEntity.noContent().build();
    }
}
