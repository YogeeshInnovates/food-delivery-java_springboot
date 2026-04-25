package com.example.online_food_delivery.service;

import com.example.online_food_delivery.dto.menu_dto.Menu_Request;
import com.example.online_food_delivery.dto.menu_dto.Menu_Response;
import com.example.online_food_delivery.exception.ResourceNotFoundException;
import com.example.online_food_delivery.exception.UnauthorizedException;
import com.example.online_food_delivery.model.MenuItems;
import com.example.online_food_delivery.model.Menu_Available_status;
import com.example.online_food_delivery.model.Restaurant;
import com.example.online_food_delivery.model.User;
import com.example.online_food_delivery.repository.MenuItemRepository;
import com.example.online_food_delivery.repository.RestaurantRepository;
import com.example.online_food_delivery.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final MenuItemRepository menuItemRepository;
    private final RestaurantRepository restaurantRepository;
    private final AuthUtil authUtil;

    public Menu_Response addMenuItem(Menu_Request request) {
        User currentUser = authUtil.currentUser();
        Restaurant restaurant = restaurantRepository.findById(request.getRestaurantId())
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found"));

        if (!restaurant.getOwner().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You are not authorized to add menu items to this restaurant");
        }

        MenuItems menuItem = MenuItems.builder()
                .name(request.getName())
                .price(request.getPrice())
                .category(request.getCategory())
                .description(request.getDescription())
                .restaurant(restaurant)
                .status(Menu_Available_status.AVAILABLE)
                .isDeleted(false)
                .build();

        MenuItems savedItem = menuItemRepository.save(menuItem);
        return mapToResponse(savedItem);
    }

    public Page<Menu_Response> getMenu(Long restaurantId, String category, Pageable pageable) {
        List<MenuItems> items;
        if (category != null && !category.isEmpty()) {
            items = menuItemRepository.findByRestaurantIdAndCategoryAndIsDeletedFalse(restaurantId, category);
        } else {
            items = menuItemRepository.findByRestaurantIdAndIsDeletedFalse(restaurantId);
        }
        
        // Manual pagination if repository doesn't support it directly with specific filtering yet
        // For "real-world" we usually update repository to findBy... (Pageable)
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), items.size());
        
        List<Menu_Response> content = items.subList(start, end).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
                
        return new PageImpl<>(content, pageable, items.size());
    }

    public Menu_Response updateMenuItem(Long itemId, Menu_Request request) {
        User currentUser = authUtil.currentUser();
        MenuItems menuItem = menuItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found"));

        if (!menuItem.getRestaurant().getOwner().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You are not authorized to update this menu item");
        }

        menuItem.setName(request.getName());
        menuItem.setPrice(request.getPrice());
        menuItem.setCategory(request.getCategory());
        menuItem.setDescription(request.getDescription());

        MenuItems updatedItem = menuItemRepository.save(menuItem);
        return mapToResponse(updatedItem);
    }

    @Transactional
    public void toggleAvailability(Long itemId) {
        User currentUser = authUtil.currentUser();
        MenuItems menuItem = menuItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found"));

        if (!menuItem.getRestaurant().getOwner().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You are not authorized to toggle availability for this item");
        }

        if (menuItem.getStatus() == Menu_Available_status.AVAILABLE) {
            menuItem.setStatus(Menu_Available_status.UNAVAILABLE);
        } else {
            menuItem.setStatus(Menu_Available_status.AVAILABLE);
        }
        menuItemRepository.save(menuItem);
    }

    @Transactional
    public void softDeleteMenuItem(Long itemId) {
        User currentUser = authUtil.currentUser();
        MenuItems menuItem = menuItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found"));

        if (!menuItem.getRestaurant().getOwner().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You are not authorized to delete this menu item");
        }

        menuItem.setDeleted(true);
        menuItemRepository.save(menuItem);
    }

    private Menu_Response mapToResponse(MenuItems item) {
        return Menu_Response.builder()
                .id(item.getId())
                .name(item.getName())
                .price(item.getPrice())
                .category(item.getCategory())
                .description(item.getDescription())
                .status(item.getStatus().name())
                .restaurantId(item.getRestaurant().getId())
                .restaurantName(item.getRestaurant().getName())
                .createdAt(item.getCreatedAt())
                .build();
    }

}
