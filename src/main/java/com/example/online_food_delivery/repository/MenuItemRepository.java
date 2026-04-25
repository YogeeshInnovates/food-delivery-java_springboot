package com.example.online_food_delivery.repository;

import com.example.online_food_delivery.model.MenuItems;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItems, Long> {
    List<MenuItems> findByRestaurantIdAndIsDeletedFalse(Long restaurantId);
    List<MenuItems> findByRestaurantIdAndCategoryAndIsDeletedFalse(Long restaurantId, String category);
}
