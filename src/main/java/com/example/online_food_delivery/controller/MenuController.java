package com.example.online_food_delivery.controller;
import com.example.online_food_delivery.dto.menu_dto.Menu_Request;
import com.example.online_food_delivery.dto.menu_dto.Menu_Response;
import com.example.online_food_delivery.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    @GetMapping("/restaurants/{id}/menu")
    public ResponseEntity<Page<Menu_Response>> getMenu(
            @PathVariable Long id,
            @RequestParam(required = false) String category,
            Pageable pageable) {
        return ResponseEntity.ok(menuService.getMenu(id, category, pageable));
    }

    @PostMapping("/restaurants/{id}/menu")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Menu_Response> addMenuItem(
            @PathVariable Long id,
            @RequestBody Menu_Request request) {
        request.setRestaurantId(id);
        return new ResponseEntity<>(menuService.addMenuItem(request), HttpStatus.CREATED);
    }

    @PutMapping("/menu/{itemId}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Menu_Response> updateMenuItem(
            @PathVariable Long itemId,
            @RequestBody Menu_Request request) {
        return ResponseEntity.ok(menuService.updateMenuItem(itemId, request));
    }

    @PatchMapping("/menu/{itemId}/availability")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Void> toggleAvailability(@PathVariable Long itemId) {
        menuService.toggleAvailability(itemId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/menu/{itemId}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Void> deleteMenuItem(@PathVariable Long itemId) {
        menuService.softDeleteMenuItem(itemId);
        return ResponseEntity.noContent().build();
    }
}
