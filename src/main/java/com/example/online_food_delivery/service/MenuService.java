package com.example.online_food_delivery.service;

import com.example.online_food_delivery.dto.menu_dto.Menu_Request;
import com.example.online_food_delivery.dto.menu_dto.Menu_Response;
import com.example.online_food_delivery.exception.ResourceNotFoundException;
import com.example.online_food_delivery.exception.UnauthorizedException;
import com.example.online_food_delivery.model.MenuCategory;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

        MenuCategory category = MenuCategory.MAIN_COURSE;
        if (request.getCategory() != null && !request.getCategory().isBlank()) {
            try {
                category = MenuCategory.valueOf(request.getCategory().toUpperCase().replace(" ", "_"));
            } catch (IllegalArgumentException ignored) {
                // default to MAIN_COURSE if invalid
            }
        }

        MenuItems menuItem = MenuItems.builder()
                .name(request.getName())
                .price(request.getPrice())
                .category(category)
                .description(request.getDescription())
                .restaurant(restaurant)
                .isVeg(request.getIsVeg() != null ? request.getIsVeg() : false)
                .status(Menu_Available_status.AVAILABLE)
                .isAvailable(true)
                .isDeleted(false)
                .build();

        MenuItems savedItem = menuItemRepository.save(menuItem);
        return mapToResponse(savedItem);
    }

    public Page<Menu_Response> getMenu(Long restaurantId, String category, Pageable pageable) {
        List<MenuItems> items;
        if (category != null && !category.isEmpty()) {
            items = menuItemRepository.findByRestaurantIdAndIsDeletedFalse(restaurantId);
            // Filter by category string match
            items = items.stream()
                    .filter(i -> i.getCategory().name().equalsIgnoreCase(category.replace(" ", "_")))
                    .collect(Collectors.toList());
        } else {
            items = menuItemRepository.findByRestaurantIdAndIsDeletedFalse(restaurantId);
        }

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
        if (request.getCategory() != null && !request.getCategory().isBlank()) {
            try {
                menuItem.setCategory(MenuCategory.valueOf(request.getCategory().toUpperCase().replace(" ", "_")));
            } catch (IllegalArgumentException ignored) {}
        }
        menuItem.setDescription(request.getDescription());
        if (request.getIsVeg() != null) menuItem.setIsVeg(request.getIsVeg());

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
            menuItem.setIsAvailable(false);
        } else {
            menuItem.setStatus(Menu_Available_status.AVAILABLE);
            menuItem.setIsAvailable(true);
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

    public Page<Menu_Response> searchMenuItems(String query, Pageable pageable) {
        Page<MenuItems> items = menuItemRepository.findByNameContainingIgnoreCaseAndIsDeletedFalse(query, pageable);
        return items.map(this::mapToResponse);
    }

    public Page<Menu_Response> getPopularItems(Pageable pageable) {
        Pageable sorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "id"));
        Page<MenuItems> items = menuItemRepository.findByIsDeletedFalseAndIsAvailableTrue(sorted);
        return items.map(this::mapToResponse);
    }

    public Menu_Response updateMenuItemImage(Long itemId, String imageUrl) {
        User currentUser = authUtil.currentUser();
        MenuItems menuItem = menuItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found"));

        if (!menuItem.getRestaurant().getOwner().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You are not authorized to update this menu item's image");
        }

        menuItem.setImageUrl(imageUrl);
        MenuItems updated = menuItemRepository.save(menuItem);
        return mapToResponse(updated);
    }

    private Menu_Response mapToResponse(MenuItems item) {
        return Menu_Response.builder()
                .id(item.getId())
                .name(item.getName())
                .price(item.getPrice())
                .category(item.getCategory().name())
                .description(item.getDescription())
                .isVeg(item.getIsVeg())
                .imageUrl(item.getImageUrl())
                .isAvailable(item.getIsAvailable())
                .status(item.getStatus().name())
                .restaurantId(item.getRestaurant().getId())
                .restaurantName(item.getRestaurant().getName())
                .createdAt(item.getCreatedAt())
                .build();
    }
}
