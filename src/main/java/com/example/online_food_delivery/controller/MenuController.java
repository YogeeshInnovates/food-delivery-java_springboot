package com.example.online_food_delivery.controller;

import com.example.online_food_delivery.dto.menu_dto.Menu_Response;
import com.example.online_food_delivery.service.MenuService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@SecurityRequirement(name = "bearerAuth")
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

    @GetMapping("/menu-items/search")
    public ResponseEntity<Page<Menu_Response>> searchMenuItems(@RequestParam String q, Pageable pageable) {
        return ResponseEntity.ok(menuService.searchMenuItems(q, pageable));
    }

    @GetMapping("/menu-items/popular")
    public ResponseEntity<Page<Menu_Response>> getPopularItems(Pageable pageable) {
        return ResponseEntity.ok(menuService.getPopularItems(pageable));
    }
}
