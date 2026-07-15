package com.example.online_food_delivery.controller;

import com.example.online_food_delivery.dto.menu_dto.Menu_Request;
import com.example.online_food_delivery.dto.menu_dto.Menu_Response;
import com.example.online_food_delivery.service.CloudinaryService;
import com.example.online_food_delivery.service.MenuService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/owner")
@PreAuthorize("hasRole('OWNER')")
@RequiredArgsConstructor
public class OwnerMenuController {

    private final MenuService menuService;
    private final CloudinaryService cloudinaryService;

    @PostMapping("/restaurants/{restaurantId}/menu-items")
    public ResponseEntity<Menu_Response> addMenuItem(
            @PathVariable Long restaurantId,
            @RequestBody Menu_Request request) {
        request.setRestaurantId(restaurantId);
        return new ResponseEntity<>(menuService.addMenuItem(request), HttpStatus.CREATED);
    }

    @PutMapping("/menu-items/{itemId}")
    public ResponseEntity<Menu_Response> updateMenuItem(
            @PathVariable Long itemId,
            @RequestBody Menu_Request request) {
        return ResponseEntity.ok(menuService.updateMenuItem(itemId, request));
    }

    @DeleteMapping("/menu-items/{itemId}")
    public ResponseEntity<Void> deleteMenuItem(@PathVariable Long itemId) {
        menuService.softDeleteMenuItem(itemId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/menu-items/{itemId}/availability")
    public ResponseEntity<Void> toggleAvailability(@PathVariable Long itemId) {
        menuService.toggleAvailability(itemId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/menu-items/{itemId}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Menu_Response> uploadMenuItemImage(
            @PathVariable Long itemId,
            @RequestParam("file") MultipartFile file) {
        String imageUrl = cloudinaryService.uploadImage(file, "menu-items/" + itemId);
        return ResponseEntity.ok(menuService.updateMenuItemImage(itemId, imageUrl));
    }
}
